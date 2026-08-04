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

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.util.newBufferedSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Class which is delegating incoming method calls to registered child listeners.
 */
internal class ProxyWebSocketListener(
    val logger: Logger = LoggerNoop,
) : WebSocketListener() {

    private var lastState = SocketState.INITIAL

    private val listeners: MutableCollection<WebSocketListener> = Collections.newSetFromMap(ConcurrentHashMap())

    private val _messageFlow = newBufferedSharedFlow<Pair<WebSocket, String>>()

    /**
     * Hot [SharedFlow] of raw WebSocket text messages. Each emission is a pair of the [WebSocket]
     * and the message text. This flow has no replay — subscribers that start late will not receive
     * earlier messages. Binary frames are not emitted.
     */
    val messageFlow: SharedFlow<Pair<WebSocket, String>> = _messageFlow.asSharedFlow()

    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        reportState(SocketState.CLOSED)
        for (listener in listeners) {
            listener.onClosed(webSocket, code, reason)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        reportState(SocketState.CLOSING)
        for (listener in listeners) {
            listener.onClosing(webSocket, code, reason)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        reportState(SocketState.CLOSED)
        for (listener in listeners) {
            listener.onFailure(webSocket, t, response)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        reportState(SocketState.OPEN)
        for (listener in listeners) {
            listener.onMessage(webSocket, text)
        }
        // onMessage is a non-suspending OkHttp callback, so messageFlow uses DROP_OLDEST to avoid
        // blocking the socket thread on a slow collector. A drop can only happen when a collector
        // falls more than the buffer behind during a burst; log it so the loss is never silent
        // (the synchronous listeners above are unaffected and still receive every frame).
        if (!_messageFlow.tryEmit(webSocket to text)) {
            logger.warning("Dropped a WebSocket message from messageFlow: a collector is overflowing the buffer")
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        reportState(SocketState.OPEN)
        for (listener in listeners) {
            listener.onMessage(webSocket, bytes)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        for (listener in listeners) {
            listener.onOpen(webSocket, response)
        }
    }

    /**
     * Reports the given [SocketState] to all listeners implementing [SocketStateListener].
     */
    fun reportState(state: SocketState) {
        if (state === lastState) return
        listeners.filterIsInstance<SocketStateListener>().forEach { listener -> listener.onStateChanged(state) }
        lastState = state
    }
}
