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
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ChatThreadsHandlerMessages(
    private val origin: ChatThreadsHandler,
    private val coroutineScope: CoroutineScope,
    identity: Logger = LoggerNoop,
) : ChatThreadsHandler by origin,
    LoggerScope by LoggerScope("ChatThreadsHandlerMessages", identity) {

    private val messages = ConcurrentHashMap<UUID, List<Message>>()
    private val watcherJobs = ConcurrentHashMap<UUID, Job>()

    // Bumped by the cache watcher whenever it writes into `messages`. The cache watcher observes
    // EventMessageCreated directly and origin.threadsFlow does not — without this, a new message
    // updates the cache silently but nothing re-emits threadsFlow to surface it.
    private val cacheVersion = MutableStateFlow(0L)

    override val threadsFlow: Flow<List<ChatThread>> = combine(
        origin.threadsFlow.onEach { threads ->
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
            messages.keys.removeIf { it !in currentIds }
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
                val cached = messages[thread.id]
                when {
                    !cached.isNullOrEmpty() -> thread.asCopyable().copy(messages = thread.messages.updateWith(cached))
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
            val cacheWatcher = origin.thread(thread.asCopyable().copy())
            val watcherScope = childScope("cacheWatcher")
            coroutineScope.safeLaunch(watcherScope) {
                cacheWatcher.threadFlow
                    .takeWhile { it.threadState !== ChatThreadState.Closed }
                    .safeCollect(watcherScope.childScope("collect")) { updatedThread ->
                        messages[updatedThread.id] = updatedThread.messages
                        cacheVersion.update { it + 1 }
                    }
            }.also { newJob = it }
        }
        newJob?.let { capturedJob ->
            capturedJob.invokeOnCompletion { watcherJobs.remove(thread.id, capturedJob) }
        }
        return origin.thread(thread)
    }
}
