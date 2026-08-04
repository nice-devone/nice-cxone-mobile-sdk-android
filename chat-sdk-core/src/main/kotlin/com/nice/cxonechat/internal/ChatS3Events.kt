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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.network.EventInS3
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.info
import com.nice.cxonechat.util.onFailure
import com.nice.cxonechat.util.onSuccess
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Request.Builder
import java.io.IOException

/**
 * Implementation of the [ChatWithParameters] which handles EventInS3 events from the server.
 */
internal class ChatS3Events(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin,
    LoggerScope by LoggerScope<Chat>(origin.entrails.logger) {

    private var listenerJob: Job? = null

    override suspend fun connect() {
        listenerJob?.cancel()
        // Scope choices:
        //   Single collector — no supervisorScope needed; sibling isolation is irrelevant with
        //     one listener, and cross-decorator isolation is handled by the SupervisorJob at
        //     the root of entrails.threading.coroutineScope.
        //   CoroutineStart.UNDISPATCHED — subscription to the hot messageFlow completes
        //     synchronously before origin.connect() opens the socket, closing a theoretical
        //     missed-emission window (messageFlow has replay = 0).
        //   safeCollect — a throw inside the collector body (e.g. an IOException escaping the
        //     inner try, or a misbehaving client callback) is logged; collection continues so
        //     subsequent S3 events are still processed.
        listenerJob = entrails.threading.coroutineScope.launch(
            entrails.threading.ioDispatcher,
            start = CoroutineStart.UNDISPATCHED,
        ) {
            socketListener.eventFlow(EventInS3).safeCollect(this@ChatS3Events) { (webSocket, event) ->
                val request = Builder()
                    .url(event.data.s3Object.url)
                    .build()

                info("Requesting s3 event: ${event.data.s3Object.url}")

                try {
                    entrails.sharedClient
                        .newCall(request)
                        .execute()
                        .onSuccess {
                            // body is non-null per OkHttp 5 typing; string() may throw
                            // IOException which is handled by the outer catch below.
                            origin.socketListener.onMessage(webSocket, body.string())
                        }
                        .onFailure {
                            error("HttpCode=$code: s3 event: ${event.data.s3Object.url}")
                            chatStateListener?.onChatRuntimeException(ServerCommunicationError(ErrorType.S3EventLoadFailed.value))
                        }
                } catch (exc: IOException) {
                    error("Error: s3 event: ${event.data.s3Object.url}", exc)
                    chatStateListener?.onChatRuntimeException(ServerCommunicationError(ErrorType.S3EventLoadFailed.value))
                }
            }
        }
        origin.connect()
    }

    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending() {
        listenerJob?.cancel()
        listenerJob = null

        origin.closeSuspending()
    }
}
