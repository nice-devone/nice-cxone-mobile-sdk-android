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

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.event.thread.SendTranscriptEvent
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.network.EventTranscriptSent
import com.nice.cxonechat.internal.socket.EventCallback.Companion.acceptResponse
import com.nice.cxonechat.internal.socket.send
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ChatThreadEventHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThread,
) : ChatThreadEventHandler {

    override suspend fun trigger(event: ChatThreadEvent): EventResponse? {
        val socket = chat.awaitSocket()

        val model = event.getModel(thread, chat.connection)
        socket.send(model)

        return when (event) {
            is SendTranscriptEvent -> awaitTranscriptResponse(event)
            else -> null
        }
    }

    private suspend fun awaitTranscriptResponse(event: SendTranscriptEvent): EventResponse {
        return suspendCancellableCoroutine { continuation ->
            val cancellable = chat.socketListener.acceptResponse(
                sent = event,
                received = EventTranscriptSent,
                errorType = ErrorType.SendingTranscriptFailed,
                failure = {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            ServerCommunicationError(ErrorType.SendingTranscriptFailed.value)
                        )
                    }
                }
            ) {
                if (continuation.isActive) {
                    continuation.resume(EventResponse.Success)
                }
            }
            continuation.invokeOnCancellation { cancellable.cancel() }
        }
    }
}
