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
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.SocketStateListener
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.info
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    private val connect: suspend () -> Unit,
) : WebSocketListener(), AutoCloseable, SocketStateListener {
    private val loggerScope = LoggerScope("ReconnectingListener", loggerScope)

    // Coroutine scope for managing asynchronous tasks.
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    // Tracks whether the WebSocket was ever successfully connected.
    private var wasEverConnected = AtomicBoolean(false)

    // Tracks the number of reconnection attempts.
    // AtomicInteger: read on OkHttp callback threads (onFailure/onClosing), incremented on coroutine dispatcher.
    @VisibleForTesting
    internal val reconnectAttempts = AtomicInteger(0)

    // Job for managing the reconnection coroutine.
    // @Volatile: read/written from both OkHttp callback threads and the coroutine dispatcher.
    @Volatile
    @VisibleForTesting
    internal var reconnectJob: Job? = null

    // True while reconnectJob is actively executing connect() (i.e. past the backoff delay).
    // @Volatile: set on the coroutine dispatcher, read on OkHttp callback threads in onConnected().
    // Guards onConnected() from cancelling an in-flight connect() — doing so would abort the
    // post-open live-chat recovery + onReady() and leave the chat stuck at Connected (DE-172157 B4).
    @Volatile
    private var connectInProgress = false

    /**
     * Tracks the current delay for reconnection attempts (in milliseconds).
     * Initialized to -1 to indicate the first attempt.
     * @Volatile: read/written from both OkHttp callback threads and the coroutine dispatcher.
     */
    @Volatile
    private var currentDelayMillis: Long = -1L

    /**
     * Function to generate a random delay in milliseconds between 1s and 5s (1000–5000ms).
     * This is used for the initial reconnection attempt.
     * Can be overridden in tests for deterministic behavior.
     */
    @VisibleForTesting
    internal var randomDelayProvider: () -> Long = ReconnectBackoff::defaultRandomDelay

    /**
     * Attempts to reconnect with exponential backoff and random initial delay.
     * The first attempt waits for 1s + random(1–5s), subsequent attempts multiply delay by 1.3.
     * Stops after MAX_RECONNECT_ATTEMPTS.
     */
    private fun attemptReconnectWithBackoff(): Unit = loggerScope.scope("attemptReconnectWithBackoff") {
        if (reconnectAttempts.get() >= ReconnectBackoff.MAX_RECONNECT_ATTEMPTS) {
            info("Max reconnect attempts reached, notifying listener of unexpected disconnect")
            chatStateListener?.onUnexpectedDisconnect()
            return
        }
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch { doReconnect() }
    }

    private suspend fun doReconnect() {
        val delayMillis = ReconnectBackoff.nextDelayMillis(
            attempt = reconnectAttempts.get(),
            previousDelayMillis = currentDelayMillis,
            randomDelay = randomDelayProvider,
        )
        currentDelayMillis = delayMillis
        loggerScope.verbose("Reconnecting in ${delayMillis}ms (attempt ${reconnectAttempts.get() + 1})")
        delay(delayMillis)
        val currentAttempt = reconnectAttempts.incrementAndGet()
        connectInProgress = true
        try {
            connect()
        } catch (e: PermanentConnectionFailureException) {
            // Non-retriable failure (e.g. expired ThirdPartyOAuth token, server-sent auth error).
            // Return terminates doReconnect() before attemptReconnectWithBackoff() is called.
            loggerScope.info("Permanent connection failure on attempt $currentAttempt, stopping retries", e)
            chatStateListener?.onUnexpectedDisconnect()
            return
        } catch (e: CancellationException) {
            // Scope shutdown (isActive=false) → propagate cancellation.
            // Socket dropped during authorization (isActive=true) → retry with backoff.
            // e.g. ChatAuthorization completes authDeferred with CancellationException for mid-auth drops.
            if (!currentCoroutineContext().isActive) throw e
            loggerScope.info("Socket failed on reconnect attempt $currentAttempt, retrying", e)
            attemptReconnectWithBackoff()
        } catch (e: TransientConnectFailureException) {
            // Transient failure: socket-open error (OkHttp onFailure) or token API outage.
            // Not a cancellation signal — retry with backoff.
            loggerScope.info("Transient connect failure on attempt $currentAttempt, retrying", e)
            attemptReconnectWithBackoff()
        } catch (logged: Exception) {
            loggerScope.warning("Unexpected exception on reconnect attempt $currentAttempt", logged)
            attemptReconnectWithBackoff()
        } finally {
            connectInProgress = false
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
            wasEverConnected.set(true)
            onConnected()
        }

    override fun onStateChanged(state: SocketState) = loggerScope.scope("SocketStateListener/onStateChanged") {
        if ((state === SocketState.CONNECTED || state === SocketState.OPEN) && !wasEverConnected.getAndSet(true)) {
            onConnected()
        }
    }

    private fun onConnected() = loggerScope.scope("ReconnectingListener/onConnected") {
        verbose("WebSocket connected")
        reconnectAttempts.set(0)
        currentDelayMillis = -1L
        // Cancel only a PENDING (still-delaying) reconnect attempt to avoid a redundant connect.
        // Never cancel a job that is actively executing connect(): the socket opening is what fires
        // this callback, and that connect() still has to run its post-open live-chat recovery and
        // onReady(). Cancelling it here aborts recovery and leaves the chat stuck at Connected
        // (the state machine never advances to Ready) — DE-172157 B4.
        if (!connectInProgress) reconnectJob?.cancel()
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
            if (!wasEverConnected.get()) {
                // Initial connection failed, notify integrator immediately
                info("Initial WebSocket connection failed, reconnection will not be attempted")
                chatStateListener?.onUnexpectedDisconnect()
            } else {
                // Reconnect with exponential backoff if allowed
                debug("WebSocket connection failed, will attempt to reconnect")
                chatStateListener?.onConnecting()
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
            if (code != WebSocketSpec.CLOSE_NORMAL_CODE && wasEverConnected.get()) {
                debug("WebSocket is closing abnormally, will attempt to reconnect")
                chatStateListener?.onConnecting()
                // Abnormal closure after a successful connection, attempt to reconnect
                attemptReconnectWithBackoff()
            }
        }

    override fun close() {
        coroutineScope.cancel("Closed")
    }

    /**
     * Aliases for existing test references — actual values/formula live in [ReconnectBackoff],
     * shared with [PreSocketReconnectScheduler].
     */
    internal companion object {
        const val MAX_RECONNECT_ATTEMPTS = ReconnectBackoff.MAX_RECONNECT_ATTEMPTS
        const val MAX_BACKOFF = ReconnectBackoff.MAX_BACKOFF
        const val INITIAL_DELAY = ReconnectBackoff.INITIAL_DELAY
    }
}
