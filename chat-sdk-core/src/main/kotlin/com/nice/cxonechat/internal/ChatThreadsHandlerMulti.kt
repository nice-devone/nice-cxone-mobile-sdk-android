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

import androidx.annotation.GuardedBy
import com.nice.cxonechat.ChatThreadEventHandlerActions.loadMetadata
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.enums.ErrorType.MetadataLoadFailed
import com.nice.cxonechat.event.FetchThreadEvent
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.info
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

internal class ChatThreadsHandlerMulti(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin, LoggerScope by LoggerScope("ChatThreadsHandlerMulti", chat.entrails.logger) {
    private val metadataRequested: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // stateLock guards the find→update→toList sequence on currentThreads so that a concurrent
    // ThreadListFetched (which replaces currentThreads) cannot swap the list between the find
    // and the subsequent trySendOrLog emission.
    private val stateLock = Any()

    @GuardedBy("stateLock")
    private var currentThreads = emptyList<ChatThreadMutable>()

    override val threadsFlow: Flow<List<ChatThread>> = channelFlow {
        scope("threadsFlow") {
            val channel = this@channelFlow
            origin.threadsFlow.safeCollect(this) { threads ->
                val mutableThreads = threads.map { it.asMutable() }
                synchronized(stateLock) { currentThreads = mutableThreads }
                trySendOrLog(channel, mutableThreads.toList(), this@scope, "thread list update")
                for (thread in mutableThreads) {
                    // add() returns true only for new entries — atomically guards against
                    // duplicate concurrent metadata requests from re-delivered ThreadListFetched.
                    if (thread.threadState != Pending && metadataRequested.add(thread.id)) {
                        val threadId = thread.id
                        val threadHandler = thread(thread.snapshot())
                        // Subscribe BEFORE sending the request — the metadata response may arrive
                        // synchronously within the same dispatcher turn as loadMetadata(); UNDISPATCHED
                        // ensures first{} is already suspended before the send begins.
                        val updateJob = launch(start = CoroutineStart.UNDISPATCHED) {
                            updateMetadata(threadHandler, threadId, channel)
                        }
                        requestMetadataForThread(threadId, threadHandler, updateJob)
                    }
                }
            }
        }
    }.onStart { refresh() }
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 1)

    private suspend fun LoggerScope.updateMetadata(
        threadHandler: ChatThreadHandler,
        threadId: UUID,
        channel: ProducerScope<List<ChatThread>>,
    ) = scope("updateMetadata") {
        try {
            val updated = withTimeoutOrNull(METADATA_LOAD_TIMEOUT) {
                verbose("Waiting for updated thread copy")
                threadHandler.threadFlow.first { it.threadState === Loaded || it.threadState === Ready }
            }
            if (updated == null) {
                metadataRequested.remove(threadId)
                warning("Metadata load timed out after $METADATA_LOAD_TIMEOUT for thread $threadId")
            } else if (updated.threadState === Loaded) {
                synchronized(stateLock) {
                    verbose("Fetching current copy of thread: $threadId")
                    val current = currentThreads.find { it.id == threadId }
                    if (current != null) {
                        current.update(updated)
                        trySendOrLog(
                            channel,
                            currentThreads.toList(),
                            this@scope,
                            "post-metadata thread update"
                        )
                        info("Thread $threadId updated with metadata")
                    } else {
                        warning("Thread $threadId disappeared after metadata load")
                    }
                }
            } else {
                debug("Thread $threadId is already ready")
            }
        } catch (cancelled: CancellationException) {
            metadataRequested.remove(threadId)
            verbose("Cancelled metadata update")
            throw cancelled
        } catch (logged: Exception) {
            metadataRequested.remove(threadId)
            warning("Metadata wait coroutine failed for thread $threadId", logged)
        }
    }

    override fun refresh(): Unit = scope("refresh") {
        metadataRequested.clear()
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerFetchThread")) {
            chat.events().trigger(FetchThreadEvent)
        }
        origin.refresh()
    }

    // Uses raw launch with custom error handling (reports ServerCommunicationError to chatStateListener)
    // instead of safeLaunch, which only logs generically. On failure, cancels the paired updateJob
    // so it stops waiting for a response that will never arrive; the CancellationException path in
    // updateMetadata then removes threadId from metadataRequested, allowing the next
    // ThreadListFetched to retry the load. This keeps remove() at a single ownership point.
    private fun requestMetadataForThread(
        threadId: UUID,
        threadHandler: ChatThreadHandler,
        updateJob: Job,
    ) = scope("requestMetadataForThread") {
        chat.entrails.threading.coroutineScope.launch {
            try {
                verbose("Fetching thread $threadId metadata")
                threadHandler.events().loadMetadata()
            } catch (cancelled: CancellationException) {
                verbose("Metadata request canceled")
                throw cancelled
            } catch (logged: Exception) {
                updateJob.cancel()
                warning("Failed to load metadata for thread", logged)
                try {
                    chat.chatStateListener?.onChatRuntimeException(
                        ServerCommunicationError(MetadataLoadFailed.value)
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (loggedListenerEx: Exception) {
                    warning("chatStateListener threw during metadata-failure notification", loggedListenerEx)
                }
            }
        }
    }

    private companion object {
        val METADATA_LOAD_TIMEOUT = 30_000L.milliseconds
    }
}
