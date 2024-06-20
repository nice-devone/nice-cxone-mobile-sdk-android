/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadEventHandlerActions.loadMetadata
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.enums.ErrorType.MetadataLoadFailed
import com.nice.cxonechat.event.FetchThreadEvent
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Ready
import java.util.UUID

internal class ChatThreadsHandlerMulti(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {
    private val metadataRequested = mutableSetOf<UUID>()

    override fun refresh() {
        metadataRequested.clear()
        chat.events().trigger(FetchThreadEvent)
        origin.refresh()
    }

    override fun threads(listener: OnThreadsUpdatedListener) = origin.threads { threads ->
        val mutableThreads = threads.map { it.asMutable() }
        for (thread in mutableThreads) {
            if (!metadataRequested.contains(thread.id)) {
                val threadHandler = thread(thread.snapshot())
                registerForThreadUpdates(threadHandler, thread, listener, threads)
                requestMetadataForThread(threadHandler, thread)
            }
        }
        listener.onThreadsUpdated(threads)
    }.also {
        refresh()
    }

    private fun registerForThreadUpdates(
        threadHandler: ChatThreadHandler,
        thread: ChatThreadMutable,
        listener: OnThreadsUpdatedListener,
        threads: List<ChatThread>,
    ) {
        var onMetadataLoaded: Cancellable? = null
        onMetadataLoaded = threadHandler.get { updatedThread ->
            if (updatedThread.threadState === Loaded || updatedThread.threadState === Ready) {
                if (updatedThread.threadState === Loaded) {
                    thread.update(updatedThread)
                    listener.onThreadsUpdated(threads)
                }
                onMetadataLoaded?.cancel()
            }
        }
    }

    private fun requestMetadataForThread(threadHandler: ChatThreadHandler, thread: ChatThreadMutable) {
        threadHandler.events().loadMetadata(listener = {
            metadataRequested.add(thread.id)
        }) {
            chat.chatStateListener?.onChatRuntimeException(
                ServerCommunicationError(
                    MetadataLoadFailed.value
                )
            )
        }
    }
}
