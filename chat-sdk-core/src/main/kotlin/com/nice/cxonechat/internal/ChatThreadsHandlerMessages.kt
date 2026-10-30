/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal class ChatThreadsHandlerMessages(
    private val origin: ChatThreadsHandler,
    private val coroutineScope: CoroutineScope,
    identity: Logger = LoggerNoop,
) : ChatThreadsHandler by origin,
    LoggerScope by LoggerScope("ChatThreadsHandlerMessages", identity) {

    // A metadata value and the tick at which it last changed. The thread list and the cache watcher both update a
    // thread's contactId and threadState, and either one can be the newer: EventMessageCreated reaches only the
    // watcher, while a SingleThread recovery (new contact, case reopened) reaches only the list. A value at tick
    // UNKNOWN is one its source did not learn from the server, so it never outranks the other source.
    private data class Stamped<T>(val value: T, val changedAt: Long)

    // The contact metadata of a thread, as the list or the cache watcher last reported it.
    private data class Metadata(
        val contactId: Stamped<String?>,
        val threadState: Stamped<ChatThreadState>,
    )

    // What the cache watcher last saw for a thread: its messages, which origin.threadsFlow does not observe, and
    // its contact metadata.
    private data class CachedThread(
        val messages: List<Message>,
        val metadata: Metadata,
    )

    // A thread instance the list delivered, with its metadata. Held weakly: an instance no collector or replay
    // still holds can never be delivered again.
    private class ListedThread(
        val source: WeakReference<ChatThread>,
        val metadata: Metadata,
    ) {
        fun inThread(thread: ChatThread) = source.get() === thread
    }

    private val clock = AtomicLong(UNKNOWN)
    private val cache = ConcurrentHashMap<UUID, CachedThread>()

    // Every instance the list delivered for a thread, so each collector reads the stamps the instance got when it
    // first arrived. A collector that lags behind would otherwise stamp older snapshots newer than what the cache
    // watcher saw since.
    private val listed = ConcurrentHashMap<UUID, List<ListedThread>>()
    private val watcherJobs = ConcurrentHashMap<UUID, Job>()

    // Bumped by the cache watcher whenever it changes an entry in `cache`. The cache watcher observes
    // EventMessageCreated directly and origin.threadsFlow does not — without this, a new message
    // updates the cache silently but nothing re-emits threadsFlow to surface it.
    private val cacheVersion = MutableStateFlow(0L)

    // threadsFlow is cold, so a list replayed to a late collector would be stamped newer than what the watcher saw
    // in the meantime. One collector, started with the first one of threadsFlow (when origin first requests the
    // list) and kept for the session, stamps each list update as it arrives; a collector then finds the replayed
    // instance already stamped. It only stamps: eviction stays with the collectors, as do origin's replay rules.
    private val listStamping = AtomicBoolean(false)

    override val threadsFlow: Flow<List<ChatThread>> = combine(
        origin.threadsFlow.onStart { startListStamping() }.onEach { threads ->
            // List-membership eviction: archived threads survive in the cache as long as the server keeps
            // them in the list; deleted/purged threads (absent from the list) are evicted.
            val currentIds = threads.mapTo(HashSet()) { it.id }
            // Cancel watchers before removing messages to minimize the window where a straggler watcher
            // write survives this emission; eventual consistency is ensured by the next eviction round.
            watcherJobs.entries.removeIf { (id, job) ->
                val evicted = id !in currentIds
                if (evicted) job.cancel(CancellationException("Thread $id evicted from list"))
                evicted
            }
            cache.keys.removeIf { it !in currentIds }
            listed.keys.removeIf { it !in currentIds }
            stampList(threads)
        },
        cacheVersion,
    ) { threads, _ -> threads }
        .map { threads ->
            threads.map { thread ->
                // Merge (not replace) whenever the cache holds content: origin's own thread.messages
                // may itself carry a ThreadMetadataLoaded preview the cache watcher never observed
                // (it starts from the thread snapshot passed to .thread(), which predates the
                // watcher's own subscription) — replacing outright would silently drop that preview.
                // Applying this whenever a cache exists (not just when origin is empty) matters
                // because origin already shows a non-empty preview for any active conversation, which
                // is the common case, not an edge case. When there is no cache at all, fall back to
                // the pre-existing behavior exactly (copy-normalize empty threads, pass non-empty
                // threads through unchanged) so callers that depend on that identity/type are unaffected.
                val cached = cache[thread.id]
                val listedMetadata = listed[thread.id]?.find { it.inThread(thread) }?.metadata
                when {
                    cached != null -> thread.asCopyable().copy(
                        messages = if (cached.messages.isEmpty()) {
                            thread.messages
                        } else {
                            thread.messages.updateWith(cached.messages)
                        },
                        // Whichever source changed a value last holds the newer one.
                        contactId = newer(listedMetadata?.contactId, cached.metadata.contactId, thread.contactId),
                        threadState = newer(listedMetadata?.threadState, cached.metadata.threadState, thread.threadState),
                    )
                    thread.messages.isEmpty() -> thread.asCopyable().copy(messages = emptyList())
                    else -> thread
                }
            }
        }

    override fun thread(thread: ChatThread): ChatThreadHandler {
        // One cache watcher per thread ID — deduplicated so repeated thread() calls for the
        // same thread don't accumulate long-lived coroutines in the chat scope.
        // Capture only the job created for a new entry so invokeOnCompletion is registered
        // once and outside computeIfAbsent — calling it inside would cause a recursive-update
        // crash on ConcurrentHashMap when the job completes synchronously (e.g. UnconfinedTestDispatcher).
        var newJob: Job? = null
        watcherJobs.computeIfAbsent(thread.id) {
            // Taken before the handler exists: it starts applying events when built, possibly before the watcher
            // subscribes, so its first emission may already be newer than the thread passed in.
            val baseline = thread.snapshotMetadata()
            val cacheWatcher = origin.thread(thread.asCopyable().copy())
            val watcherScope = childScope("cacheWatcher")
            coroutineScope.safeLaunch(watcherScope) {
                cacheWatcher.threadFlow
                    // Keep the emission that closes the thread, so the list shows it closed, then stop.
                    .transformWhile { updatedThread ->
                        emit(updatedThread)
                        updatedThread.threadState !== ChatThreadState.Closed
                    }
                    .safeCollect(watcherScope.childScope("collect")) { updatedThread ->
                        var changed = false
                        cache.compute(updatedThread.id) { _, previous ->
                            CachedThread(updatedThread.messages, (previous?.metadata ?: baseline).watched(updatedThread))
                                .also { changed = it != previous }
                        }
                        if (changed) cacheVersion.update { it + 1 }
                    }
            }.also { newJob = it }
        }
        newJob?.let { capturedJob ->
            capturedJob.invokeOnCompletion { watcherJobs.remove(thread.id, capturedJob) }
        }
        return origin.thread(thread)
    }

    private fun startListStamping() {
        if (!listStamping.compareAndSet(false, true)) return
        val stampingScope = childScope("listStamping")
        coroutineScope.safeLaunch(stampingScope) {
            origin.threadsFlow.safeCollect(stampingScope.childScope("collect")) { stampList(it) }
        }
    }

    // Idempotent: an instance already stamped keeps its ticks unless one of its values changed in place since.
    private fun stampList(threads: List<ChatThread>) = threads.forEach { thread ->
        listed.compute(thread.id) { _, previous ->
            val retained = previous.orEmpty().filter { it.source.get() != null }
            val stamped = retained.find { it.inThread(thread) }
            val metadata = thread.listedMetadata(stamped?.metadata)
            retained - listOfNotNull(stamped) + ListedThread(WeakReference(thread), metadata)
        }
    }

    // A new instance is a new server snapshot (a fetched list, a SingleThread recovery), so each value it knows is
    // newer than the cache, even one that did not change (recovery always reports Ready). An instance already
    // stamped only counts as newer where a value changed in place. A fetched list knows neither the contact nor the
    // case state: it has no contactId and reports Received, then Loaded once the metadata loads.
    private fun ChatThread.listedMetadata(previous: Metadata?) = Metadata(
        contactId = previous?.contactId.listed(contactId, known = contactId != null),
        threadState = previous?.threadState.listed(threadState, known = threadState !in PLACEHOLDER_STATES),
    )

    private fun <T> Stamped<T>?.listed(value: T, known: Boolean): Stamped<T> = when {
        this != null && this.value == value -> this
        known -> Stamped(value, clock.incrementAndGet())
        else -> Stamped(value, UNKNOWN)
    }

    // The thread passed to thread() is not a server event, so the watcher starts from its values at UNKNOWN.
    private fun ChatThread.snapshotMetadata() = Metadata(
        contactId = Stamped(contactId, UNKNOWN),
        threadState = Stamped(threadState, UNKNOWN),
    )

    // A value is newer only when it changes, so re-reporting an old value does not make it newer.
    private fun Metadata.watched(thread: ChatThread) = Metadata(
        contactId = contactId.watched(thread.contactId),
        threadState = threadState.watched(thread.threadState),
    )

    private fun <T> Stamped<T>.watched(value: T): Stamped<T> =
        if (this.value == value) this else Stamped(value, clock.incrementAndGet())

    private companion object {
        private const val UNKNOWN = 0L

        private val PLACEHOLDER_STATES = setOf(ChatThreadState.Received, ChatThreadState.Loaded)

        @JvmStatic
        private fun <T> newer(listed: Stamped<T>?, cached: Stamped<T>, fallback: T): T = when {
            listed == null -> fallback
            cached.changedAt > listed.changedAt -> cached.value
            else -> listed.value
        }
    }
}
