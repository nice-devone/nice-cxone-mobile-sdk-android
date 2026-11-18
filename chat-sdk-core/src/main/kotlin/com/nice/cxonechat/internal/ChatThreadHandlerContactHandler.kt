/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.internal.model.network.EventContactHandlerChanged
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Wraps origin [ChatThreadHandler] and emits via [threadFlow] when [EventContactHandlerChanged] or
 * [EventCaseStatusChanged] reports a handler for this thread, and in MultiThread mode when [EventCaseStatusChanged]
 * closes or reopens its case. An event that changes neither is ignored.
 * [EventCaseStatusChanged] is left to the LiveChat handler in LiveChat mode.
 */
internal class ChatThreadHandlerContactHandler(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin, LoggerScope by LoggerScope("ChatThreadHandlerContactHandler", chat.entrails.logger) {

    override val threadFlow: Flow<ChatThread> = merge(
        origin.threadFlow,
        chat.socketListener.eventFlow(EventContactHandlerChanged)
            .mapNotNull { (_, event) ->
                if (!event.inThread(thread)) return@mapNotNull null
                val handler = event.handler
                if (handler == null) {
                    warning("Ignoring contact-handler-changed event without a handler")
                    return@mapNotNull null
                }
                try {
                    thread += thread.asCopyable().copy(contactHandler = handler)
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle contact-handler-changed event in threadFlow", expected)
                    null
                }
            },
        chat.socketListener.eventFlow(EventCaseStatusChanged)
            .mapNotNull { (_, event) ->
                // LiveChat applies case status and handler in one step in ChatThreadHandlerLiveChat.
                if (chat.chatMode === LiveChat || !event.inThread(thread)) return@mapNotNull null
                try {
                    var updated = false
                    var closed = false
                    // MultiThread applies the case status like its thread list, so the open thread and the list agree.
                    if (chat.chatMode === MultiThread) {
                        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event) { updated = true }
                        // A handler opened from a threadsFlow entry shares the list's thread, which the list may
                        // already have closed; emit the close anyway so threadFlow sees it.
                        closed = event.status === Closed && thread.threadState === ChatThreadState.Closed
                    }
                    val handler = event.contactHandler
                    if (!updated && handler != null) {
                        thread += thread.asCopyable().copy(contactHandler = handler)
                        updated = true
                    }
                    if (updated || closed) get() else null
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle case-status-changed event in threadFlow", expected)
                    null
                }
            },
    )
}
