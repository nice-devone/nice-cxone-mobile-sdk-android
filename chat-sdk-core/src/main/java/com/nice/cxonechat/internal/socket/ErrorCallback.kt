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

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.internal.model.ErrorModel
import com.nice.cxonechat.internal.serializer.Default.serializer
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Simple [WebSocketListener] which attempts to deserialize [ErrorModel] from incoming messages and if the
 * [ErrorModel.Error.errorCode] matches the supplied [errorType], the element will be passed back as [onError] callback.
 *
 * @param errorType The [ErrorType] which will be used for callback.
 */
internal abstract class ErrorCallback(
    private val errorType: ErrorType,
) : WebSocketListener() {

    override fun onMessage(webSocket: WebSocket, text: String) {
        val errorMessage: ErrorModel? = serializer.runCatching { fromJson(text, ErrorModel::class.java) }.getOrNull()
        if (errorMessage?.error?.errorCode == errorType) {
            onError(webSocket)
        }
    }

    /**
     * Callback which is invoked when websocket receives message with [ErrorModel] which have matching
     * [ErrorModel.Error.errorCode] to the supplied [errorType].
     *
     * @param webSocket [WebSocket] instance received in the [WebSocketListener.onMessage] callback.
     */
    abstract fun onError(webSocket: WebSocket)

    internal companion object {

        /**
         * Create and register [ErrorCallback] for supplied [errorType].
         *
         * @receiver [ProxyWebSocketListener] which will be used for registration of the [ErrorCallback] listener.
         * @return A [Cancellable] instance which will cancel the [ErrorCallback] registration.
         */
        internal inline fun ProxyWebSocketListener.addErrorCallback(
            errorType: ErrorType,
            crossinline callback: WebSocket.() -> Unit,
        ): Cancellable {
            val listener = object : ErrorCallback(errorType) {
                override fun onError(webSocket: WebSocket) = callback(webSocket)
            }
            addListener(listener)
            return Cancellable { removeListener(listener) }
        }
    }
}
