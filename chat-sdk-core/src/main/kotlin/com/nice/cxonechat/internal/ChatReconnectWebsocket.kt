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

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.log.LoggerScope
import kotlinx.coroutines.runBlocking

/**
 * A decorator class for `ChatWithParameters` that adds reconnection logic to the WebSocket.
 *
 * This class wraps an existing `ChatWithParameters` instance and provides additional functionality
 * for handling WebSocket reconnections using the `ReconnectingListener`. It also implements the
 * `LoggerScope` interface for logging purposes.
 *
 * @param origin The original `ChatWithParameters` instance being decorated.
 */
internal class ChatReconnectWebsocket(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin, LoggerScope by LoggerScope("ChatReconnectWebsocket", origin.entrails.logger) {

    // Guards the compound re-arm (connect) / dispose (close) of reconnectListener +
    // reconnectListenerDisposed. connect() is suspend (runs on a dispatcher, including the autonomous
    // reconnect path) while close() overrides AutoCloseable.close() and cannot be suspend, so a Mutex
    // can't be shared between them. The critical sections are tiny and non-suspending — the suspending
    // origin.connect()/origin.close() are called OUTSIDE the lock — so a plain synchronized block is
    // the correct coordinator. @Volatile is insufficient here: it gives visibility but not atomicity
    // for this check-then-act across two fields.
    private val listenerLock = Any()

    // Listener responsible for managing WebSocket reconnection logic. Mutated/read only under listenerLock.
    @VisibleForTesting
    internal var reconnectListener = newReconnectingListener()
        private set

    // Set by close(); tells the next connect() that the previous listener's private scope was
    // disposed and a fresh listener must be armed. The auto-reconnect path (listener invoking
    // connect() after an unexpected disconnect, without close()) must NOT replace the listener —
    // doing so would cancel the very coroutine driving the reconnection.
    private var reconnectListenerDisposed = false

    init {
        socketListener.addListener(reconnectListener)
    }

    override suspend fun connect() {
        synchronized(listenerLock) {
            if (reconnectListenerDisposed) {
                reconnectListenerDisposed = false
                reconnectListener = newReconnectingListener().also(socketListener::addListener)
            }
        }
        origin.connect()
    }

    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending() {
        synchronized(listenerLock) {
            reconnectListenerDisposed = true
            reconnectListener.close()
            socketListener.removeListener(reconnectListener)
        }
        origin.closeSuspending()
    }

    private fun newReconnectingListener() = ReconnectingListener(
        dispatcher = origin.entrails.threading.ioDispatcher,
        chatStateListener = chatStateListener,
        loggerScope = this,
        connect = this::connect
    )
}
