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
import com.nice.cxonechat.Chat
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.network.EventInS3
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.info
import com.nice.cxonechat.util.onFailure
import com.nice.cxonechat.util.onSuccess
import okhttp3.Request.Builder
import java.io.IOException

/**
 * Implementation of the [ChatWithParameters] which handles EventInS3 events from the server.
 */
internal class ChatS3Events(
    private val origin: ChatWithParameters
) : ChatWithParameters by origin,
    LoggerScope by LoggerScope<Chat>(origin.entrails.logger) {

    var cancellable: Cancellable? = null

    override fun connect(): Cancellable = origin.connect().also {
        cancellable = origin.socketListener.addCallback(EventInS3) { event ->
            val request = Builder()
                .url(event.data.s3Object.url)
                .build()

            info("Requesting s3 event: ${event.data.s3Object.url}")

            try {
                entrails.sharedClient
                    .newCall(request)
                    .execute()
                    .onSuccess {
                        val socket = requireNotNull(origin.socket)
                        val body = requireNotNull(body?.string())

                        origin.socketListener.onMessage(socket, body)
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

    override fun close() {
        cancellable?.cancel()
        cancellable = null

        origin.close()
    }
}
