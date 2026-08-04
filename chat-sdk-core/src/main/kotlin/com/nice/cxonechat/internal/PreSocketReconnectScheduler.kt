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
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Schedules a backoff-limited automatic retry of [connect] after a pre-socket REST connect failure
 * (i.e. [ChannelAvailabilityFailedException]), so LiveChat recovers once connectivity returns instead
 * of waiting solely on the UI to re-invoke [connect].
 *
 * Owns its own [CoroutineScope] rather than sharing [ChatInstanceProvider]'s — that field is
 * reassigned by `signOut()`, and a scheduler holding the original scope would keep retrying against a
 * permanently-cancelled job after any sign-out/prepare/connect cycle. This scheduler's own scope is
 * never explicitly torn down either, for the same reason: it is tied 1:1 to the lifetime of the
 * owning [ChatInstanceProvider] instance, which itself has no explicit "this whole instance is being
 * discarded" hook — only [ChatInstanceProvider.signOut]/[ChatInstanceProvider.close]/
 * [ChatInstanceProvider.cancel], which call [reset] here, not a teardown of the scope itself.
 *
 * This class's scheduling/bookkeeping shape (attempt counter, delay field, job handle,
 * cancel-then-relaunch) intentionally mirrors [ReconnectingListener]'s, rather than sharing a common
 * coordinator with it — [ReconnectingListener] is a `WebSocketListener` whose retry logic is entangled
 * with socket-callback state (`wasEverConnected`, `connectInProgress`) that has no equivalent here, so
 * a shared abstraction would either leak that socket-specific state into this class or become a leaky
 * abstraction neither class fits cleanly. The part that MUST stay identical between them for
 * correctness — the actual backoff formula — already is: both delegate to [ReconnectBackoff], so a
 * fix to the formula itself can't drift between the two call sites even though the surrounding
 * scheduling glue is duplicated.
 *
 * @param dispatcher The coroutine dispatcher used for launching the retry coroutine.
 * @param loggerScope A scope for logging purposes.
 * @param connect The public `ChatInstanceProvider.connect()` to retry — its own `assertState` guard
 * is the single source of truth for whether a retry is still valid, so a resulting
 * [InvalidStateException] (e.g. the UI already reconnected) is caught and dropped as benign.
 * @param onExhausted Invoked once [ReconnectBackoff.MAX_RECONNECT_ATTEMPTS] is reached with no
 * successful reconnect, mirroring [ReconnectingListener]'s analogous give-up signal so the state
 * listener is notified consistently regardless of which reconnect mechanism gave up.
 */
internal class PreSocketReconnectScheduler(
    dispatcher: CoroutineDispatcher,
    loggerScope: LoggerScope,
    private val connect: () -> Unit,
    private val onExhausted: () -> Unit,
) {
    private val loggerScope = LoggerScope("PreSocketReconnectScheduler", loggerScope)
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Guards `retryJob`/`currentDelayMillis`/`generation` and makes each scheduled retry's
     * generation check and [connect] call atomic with [resetLocked] — a plain `synchronized` monitor
     * (not [kotlinx.coroutines.sync.Mutex]) because [reset] must stay callable from the non-suspend
     * public functions that call it (`cancel()`/`close()`/`signOut()`/etc. are real API, not
     * suspending).
     *
     * [connect] itself is deliberately called while still holding this lock, even though it
     * transitively runs `ChatInstanceProvider.advanceState`'s synchronous listener notification
     * (arbitrary integrator code, not guaranteed fast) — the actual network I/O runs later, inside
     * the coroutine `connect()` launches, never under this lock. Releasing the lock before calling
     * [connect] would reopen the exact race this class exists to close: a `reset()` from
     * `cancel()`/`close()`/`signOut()` landing between the generation check and the call would let
     * a stale retry fire `connect()` against a session the caller already gave up on — `Prepared`
     * (the state after `cancel()`) is a valid `connect()` state, so that retry would silently
     * reconnect it.
     *
     * Direct callers keep nesting as `lock` -> `ChatInstanceProvider.stateSyncLock`, never the
     * reverse: `cancel()`/`close()`/`signOut()`/etc. all call [reset] (`lock`) strictly before
     * `advanceState` (`stateSyncLock`). This does NOT hold for a re-entrant caller: `advanceState`
     * holds `stateSyncLock` across its own listener notification, so a `Listener.onChatStateChanged`
     * implementation that synchronously calls `cancel()`/`close()`/`signOut()` inverts the order to
     * `stateSyncLock` -> `lock` on that thread, which can deadlock against this retry coroutine's
     * `lock` -> `stateSyncLock`. Calling those APIs synchronously from a state-listener callback is
     * unsupported for this reason — tracked for a structural fix in DE-173388 (don't remove this
     * caveat without closing it).
     */
    private val lock = Any()

    /** Number of pre-socket reconnect attempts. An `AtomicInteger`, safe on its own without `lock`. */
    @VisibleForTesting
    internal val attempts = AtomicInteger(0)

    /** Mutated only under `lock`. */
    @VisibleForTesting
    internal var currentDelayMillis: Long = -1L

    /** Job for the scheduled retry coroutine. Mutated only under `lock`. */
    @VisibleForTesting
    internal var retryJob: Job? = null

    /**
     * Monotonically incremented by [resetLocked]. Each scheduled retry captures the value at
     * schedule time and re-checks it, under the same `lock`, right before calling [connect] —
     * making "is this retry still current" and "call connect" one atomic step, so a concurrent
     * [reset] can't land in between.
     */
    @VisibleForTesting
    internal var generation = 0

    /** Overridable jitter source for deterministic tests. */
    @VisibleForTesting
    internal var randomDelayProvider: () -> Long = ReconnectBackoff::defaultRandomDelay

    /**
     * Schedules a single backoff-delayed retry of [connect]. Called from
     * [ChatInstanceProvider.doConnect]'s [ChannelAvailabilityFailedException] catch block.
     */
    fun onPreSocketConnectFailure(cause: ChannelAvailabilityFailedException): Unit =
        loggerScope.scope("onPreSocketConnectFailure") {
            // onExhausted() is deferred until after the lock is released: it calls back into
            // ChatInstanceProvider.onUnexpectedDisconnect(), which notifies external listeners —
            // arbitrary, potentially slow user code must not run while `lock` is held.
            val exhausted = synchronized(lock) {
                if (attempts.get() >= ReconnectBackoff.MAX_RECONNECT_ATTEMPTS) {
                    warning("Max pre-socket reconnect attempts reached, giving up", cause)
                    // Re-arms for the next independent attempt instead of disabling retry for this scheduler's whole lifetime.
                    resetLocked()
                    true
                } else {
                    scheduleRetryLocked()
                    false
                }
            }
            if (exhausted) onExhausted()
        }

    /** Schedules the next backoff-delayed retry. Called from [onPreSocketConnectFailure] under `lock`. */
    private fun LoggerScope.scheduleRetryLocked() {
        val delayMillis = ReconnectBackoff.nextDelayMillis(
            attempt = attempts.get(),
            previousDelayMillis = currentDelayMillis,
            randomDelay = randomDelayProvider,
        )
        currentDelayMillis = delayMillis
        verbose("Retrying pre-socket connect in ${delayMillis}ms (attempt ${attempts.get() + 1})")
        val scheduledGeneration = generation
        retryJob?.cancel()
        retryJob = coroutineScope.launch {
            delay(delayMillis)
            ensureActive()
            synchronized(lock) {
                if (generation != scheduledGeneration) {
                    debug("Skipping scheduled retry: superseded by a reset()")
                    return@launch
                }
                attempts.incrementAndGet()
                try {
                    connect()
                } catch (expected: InvalidStateException) {
                    // State already advanced elsewhere; connect()'s own `assertState` is authoritative.
                    debug("Skipping scheduled retry: state already advanced", expected)
                }
            }
        }
    }

    /**
     * Cancels any pending retry and clears attempt/delay state. Call on any successful (re)connect
     * ([ChatInstanceProvider.onConnected]/[ChatInstanceProvider.onReady]) or explicit lifecycle exit
     * ([ChatInstanceProvider.cancel]/[ChatInstanceProvider.close]/[ChatInstanceProvider.signOut]).
     */
    fun reset() {
        synchronized(lock) {
            resetLocked()
        }
    }

    /** [reset]'s body, for callers that already hold `lock` (`synchronized` is reentrant). */
    private fun resetLocked() {
        generation++
        retryJob?.cancel()
        retryJob = null
        attempts.set(0)
        currentDelayMillis = -1L
    }
}
