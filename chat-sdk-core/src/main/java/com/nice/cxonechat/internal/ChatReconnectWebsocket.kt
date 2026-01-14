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

import com.nice.cxonechat.log.LoggerScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A decorator class for `ChatWithParameters` that adds reconnection logic to the WebSocket.
 *
 * This class wraps an existing `ChatWithParameters` instance and provides additional functionality
 * for handling WebSocket reconnections using the `ReconnectingListener`. It also implements the
 * `LoggerScope` interface for logging purposes.
 *
 * @param origin The original `ChatWithParameters` instance being decorated.
 * @param dispatcher The coroutine dispatcher used for reconnection logic. Defaults to `Dispatchers.IO`.
 */
internal class ChatReconnectWebsocket(
    private val origin: ChatWithParameters,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatWithParameters by origin, LoggerScope by LoggerScope("ChatReconnectWebsocket", origin.entrails.logger) {

    // Listener responsible for managing WebSocket reconnection logic.
    private val reconnectListener = ReconnectingListener(dispatcher, chatStateListener, this, this::connect)

    init {
        socketListener.addListener(reconnectListener)
    }

    override fun close() {
        reconnectListener.close()
        socketListener.removeListener(reconnectListener)
        origin.close()
    }
}
