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

import com.nice.cxonechat.FakeChatStateListener
import com.nice.cxonechat.enums.ErrorType.SendingMessageFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.server.ServerResponse
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Verifies [ChatServerErrorReporting]'s own `callbacksJob` lifecycle: cancelled by
 * [ChatServerErrorReporting.closeSuspending], and re-armed rather than accumulated by
 * [ChatServerErrorReporting.connect] — attributable to the decorator and nothing else.
 *
 * The decorator is exercised in isolation rather than through a built [Chat]: `callbacksJob` is
 * launched into `entrails.threading.coroutineScope`, and [ChatImpl.closeSuspending] cancels every
 * child of that same scope at close. Going through the full chat therefore proves nothing about
 * this decorator — the collectors would die from [ChatImpl]'s scope sweep even with
 * `callbacksJob?.cancel()` deleted.
 *
 * Here the wrapped origin is a mock whose `closeSuspending()` does nothing, so the shared scope
 * stays active across the close. The close test asserts that the scope is still active, which is
 * what makes a passing result attributable to the decorator's own cancellation. The reconnect test
 * rests on a different argument — one report rather than two — so it needs no such assertion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatServerErrorReportingJobLifecycleTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sharedScope: CoroutineScope
    private lateinit var socketListener: ProxyWebSocketListener
    private lateinit var stateListener: FakeChatStateListener
    private lateinit var errorReporting: ChatServerErrorReporting

    private val webSocket: WebSocket = mockk(relaxed = true)

    @Before
    fun prepare() {
        // A SupervisorJob standing in for the per-session scope, deliberately not tied to the
        // decorator's lifecycle — only the decorator itself may stop the collectors.
        sharedScope = CoroutineScope(testDispatcher + SupervisorJob())
        socketListener = ProxyWebSocketListener()
        stateListener = FakeChatStateListener()

        val proxyListener = socketListener
        val origin: ChatWithParameters = mockk(relaxed = true) {
            every { entrails } returns mockk(relaxed = true) {
                every { logger } returns LoggerNoop
                every { threading } returns mockk(relaxed = true) {
                    every { coroutineScope } returns sharedScope
                }
            }
            every { socketListener } returns proxyListener
            every { chatStateListener } returns stateListener
        }

        errorReporting = ChatServerErrorReporting(origin)
    }

    @After
    fun cleanup() {
        sharedScope.cancel()
    }

    /** Positive control: without this, the post-close assertion could pass vacuously. */
    @Test
    fun `errors are reported to chatStateListener while connected`() = runTest(testDispatcher) {
        errorReporting.connect()

        serverSendsError(SendingMessageFailed.value)

        assertEquals(1, stateListener.onChatRuntimeExceptions.size)
        val reported = stateListener.onChatRuntimeExceptions.single()
        assertIs<ServerCommunicationError>(reported)
        assertEquals(SendingMessageFailed.value, reported.message)
    }

    @Test
    fun `errors are not reported after closeSuspending while the shared scope stays active`() = runTest(testDispatcher) {
        errorReporting.connect()
        serverSendsError(SendingMessageFailed.value)
        val reportedBeforeClose = stateListener.onChatRuntimeExceptions.size
        assertEquals(1, reportedBeforeClose, "Pre-condition: reporting must be live before close")

        errorReporting.closeSuspending()

        // Deliberately the same error type as the pre-close probe: that makes the assertion above
        // double as the positive control for this exact path. A different type could stop being
        // collected at all (dropped from connect()'s errorType list) and this test would still
        // pass — no report, but because nothing was listening rather than because the job died.
        serverSendsError(SendingMessageFailed.value)

        assertEquals(
            reportedBeforeClose,
            stateListener.onChatRuntimeExceptions.size,
            "closeSuspending() must cancel callbacksJob so no error reaches a stale chatStateListener",
        )
        assertTrue(
            sharedScope.isActive,
            "The shared scope must survive the close, otherwise the assertion above would hold " +
                "even with callbacksJob?.cancel() removed",
        )
    }

    /**
     * The reconnect-accumulation counterpart: `connect()` cancels the previous `callbacksJob`
     * before arming a new one. Without it each reconnect leaves an extra live collector per error
     * type, so one server error frame fires `onChatRuntimeException` once per accumulated
     * collector and the integrator sees duplicate [ServerCommunicationError]s.
     */
    @Test
    fun `reconnect cancels the previous callbacksJob so an error is reported once`() = runTest(testDispatcher) {
        errorReporting.connect()
        errorReporting.connect()

        serverSendsError(SendingMessageFailed.value)

        assertEquals(
            1,
            stateListener.onChatRuntimeExceptions.size,
            "connect() must cancel the previous callbacksJob so collectors do not accumulate",
        )
    }

    private fun serverSendsError(errorCode: String) {
        socketListener.onMessage(webSocket, ServerResponse.ErrorResponse(errorCode))
    }
}
