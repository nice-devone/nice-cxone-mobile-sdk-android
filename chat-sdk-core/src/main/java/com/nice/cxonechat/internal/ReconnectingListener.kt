/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.SocketStateListener
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.info
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A WebSocket listener that handles reconnection logic with exponential backoff.
 *
 * @param dispatcher The coroutine dispatcher used for launching coroutines.
 * @param chatStateListener A listener to notify about chat state changes on errors.
 * @param loggerScope A scope for logging purposes.
 * @param connect A function to establish a WebSocket connection.
 */
internal class ReconnectingListener(
    dispatcher: CoroutineDispatcher,
    private val chatStateListener: ChatStateListener?,
    loggerScope: LoggerScope,
    private val connect: () -> Cancellable,
) : WebSocketListener(), AutoCloseable, SocketStateListener {
    private val loggerScope = LoggerScope("ReconnectingListener", loggerScope)

    // Coroutine scope for managing asynchronous tasks.
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    // Tracks whether the WebSocket was ever successfully connected.
    private var wasEverConnected = AtomicBoolean(false)

    // Tracks the number of reconnection attempts.
    private var reconnectAttempts = 0

    // Job for managing the reconnection coroutine.
    @VisibleForTesting
    internal var reconnectJob: Job? = null

    private var cancellable: Cancellable? = null

    /**
     * Tracks the current delay for reconnection attempts (in milliseconds).
     * Initialized to -1 to indicate the first attempt.
     */
    private var currentDelayMillis: Long = -1L

    /**
     * Function to generate a random delay in milliseconds between 1s and 5s (1000–5000ms).
     * This is used for the initial reconnection attempt.
     * Can be overridden in tests for deterministic behavior.
     */
    @VisibleForTesting
    internal var randomDelayProvider: () -> Long = { (MIN_RANDOM_DELAY..MAX_RANDOM_DELAY).random() }

    /**
     * Attempts to reconnect with exponential backoff and random initial delay.
     * The first attempt waits for 1s + random(1–5s), subsequent attempts multiply delay by 1.3.
     * Stops after MAX_RECONNECT_ATTEMPTS.
     */
    private fun attemptReconnectWithBackoff(): Unit = loggerScope.scope("attemptReconnectWithBackoff") {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            // Calculate delay
            val delayMillis = if (reconnectAttempts == 0) {
                // First attempt: 1s + random(1–5s)
                INITIAL_DELAY + randomDelayProvider()
            } else {
                // Subsequent attempts: exponential backoff
                (currentDelayMillis * 1.3).toLong().coerceAtMost(MAX_BACKOFF)
            }
            currentDelayMillis = delayMillis
            verbose("Reconnecting in ${delayMillis}ms (attempt ${reconnectAttempts + 1})")
            delay(delayMillis)
            reconnectAttempts++
            try {
                cancellable = connect()
            } catch (ignored: Exception) {
                info("Failed to reconnect on attempt $reconnectAttempts", ignored)
                attemptReconnectWithBackoff()
            }
        }
    }

    /**
     * Called when the WebSocket connection is successfully opened.
     * Resets the reconnection attempts and cancels any ongoing reconnection job.
     *
     * @param webSocket The WebSocket instance.
     * @param response The server's response to the WebSocket handshake.
     */
    override fun onOpen(webSocket: WebSocket, response: Response): Unit =
        loggerScope.scope("WebSocketListener/onOpen") {
            verbose("WebSocket connection opened")
            onConnected()
        }

    override fun onStateChanged(state: SocketState) = loggerScope.scope("SocketStateListener/onStateChanged") {
        if ((state === SocketState.CONNECTED || state === SocketState.OPEN) && !wasEverConnected.getAndSet(true)) {
            onConnected()
        }
    }

    private fun onConnected() = loggerScope.scope("ReconnectingListener/onConnected") {
        verbose("WebSocket connected")
        cancellable = null
        wasEverConnected.set(true)
        reconnectAttempts = 0
        currentDelayMillis = -1L
        reconnectJob?.cancel()
    }

    /**
     * Called when the WebSocket connection fails.
     * If the WebSocket was never connected, notifies the listener of the failure.
     * Otherwise, attempts to reconnect with exponential backoff.
     *
     * @param webSocket The WebSocket instance.
     * @param t The throwable representing the failure.
     * @param response The server's response, if available.
     */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?): Unit =
        loggerScope.scope("WebSocketListener/onFailure") {
            cancellable = null
            if (!wasEverConnected.get()) {
                // Initial connection failed, notify integrator immediately
                info("Initial WebSocket connection failed, reconnection will not be attempted")
                chatStateListener?.onUnexpectedDisconnect()
            } else {
                // Reconnect with exponential backoff if allowed
                debug("WebSocket connection failed, will attempt to reconnect")
                attemptReconnectWithBackoff()
            }
        }

    /**
     * Called when the WebSocket connection is closed by remote peer.
     * If the closure is abnormal and the WebSocket was previously connected, attempts to reconnect.
     *
     * @param webSocket The WebSocket instance.
     * @param code The closure code.
     * @param reason The reason for closure.
     */
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) =
        loggerScope.scope("WebSocketListener/onClosing") {
            cancellable = null
            if (code != WebSocketSpec.CLOSE_NORMAL_CODE && wasEverConnected.get()) {
                debug("WebSocket is closing abnormally, will attempt to reconnect")
                // Abnormal closure after a successful connection, attempt to reconnect
                attemptReconnectWithBackoff()
            }
        }

    override fun close() {
        cancellable?.cancel()
        coroutineScope.cancel("Closed")
    }

    internal companion object {
        // Maximum number of reconnection attempts allowed.
        const val MAX_RECONNECT_ATTEMPTS = 20

        // Maximum delay for exponential backoff (in milliseconds).
        const val MAX_BACKOFF = 500_000L

        // Initial delay for the first reconnection attempt (in milliseconds).
        const val INITIAL_DELAY = 1000L

        // Minimum random delay for the first reconnection attempt (in milliseconds).
        const val MIN_RANDOM_DELAY = 1000L

        // Maximum random delay for the first reconnection attempt (in milliseconds).
        const val MAX_RANDOM_DELAY = 5000L
    }
}
