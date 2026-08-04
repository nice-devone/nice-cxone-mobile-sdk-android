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

import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.errorFlow
import com.nice.cxonechat.internal.socket.EventCallback.Companion.awaitEventSuspend
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import okio.ByteString
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the Flow-based WebSocket event infrastructure.
 * Covers [ProxyWebSocketListener.messageFlow], [EventCallback.Companion.eventFlow],
 * [EventCallback.Companion.awaitEventSuspend], and [ErrorCallback.Companion.errorFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class WebSocketFlowTest {

    private val proxyListener = ProxyWebSocketListener()
    private val mockSocket: WebSocket = mockk(relaxed = true)

    // -- messageFlow --

    @Test
    fun `messageFlow emits text messages`() = runTest {
        val received = mutableListOf<Pair<WebSocket, String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.messageFlow.collect { received.add(it) }
        }

        proxyListener.onMessage(mockSocket, "hello")

        assertEquals(1, received.size)
        assertEquals(mockSocket, received[0].first)
        assertEquals("hello", received[0].second)
        job.cancel()
    }

    @Test
    fun `messageFlow does not replay to late subscribers`() = runTest {
        proxyListener.onMessage(mockSocket, "before-subscribe")

        val received = mutableListOf<Pair<WebSocket, String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.messageFlow.collect { received.add(it) }
        }

        assertTrue(received.isEmpty(), "Late subscriber should not receive messages emitted before subscription")
        job.cancel()
    }

    @Test
    fun `messageFlow delivers to multiple concurrent collectors`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.messageFlow.collect { received1.add(it.second) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.messageFlow.collect { received2.add(it.second) }
        }

        proxyListener.onMessage(mockSocket, "fan-out")

        assertEquals(listOf("fan-out"), received1)
        assertEquals(listOf("fan-out"), received2)
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `messageFlow does not emit binary frames`() = runTest {
        val received = mutableListOf<Pair<WebSocket, String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.messageFlow.collect { received.add(it) }
        }

        proxyListener.onMessage(mockSocket, ByteString.EMPTY)

        assertTrue(received.isEmpty(), "messageFlow should not emit for binary WebSocket frames")
        job.cancel()
    }

    // -- eventFlow --

    @Test
    fun `eventFlow emits matching events`() = runTest {
        val threadId = UUID.randomUUID()
        val json = threadUpdatedJson(threadId)

        val events = mutableListOf<EventThreadUpdated>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.eventFlow(EventThreadUpdated).collect { (_, event) ->
                events.add(event)
            }
        }

        proxyListener.onMessage(mockSocket, json)

        assertEquals(1, events.size)
        assertEquals(threadId, events[0].postback.data?.threadId)
        job.cancel()
    }

    @Test
    fun `eventFlow ignores non-matching event types`() = runTest {
        val errorJson = errorJson(ErrorType.SendingMessageFailed)

        val events = mutableListOf<EventThreadUpdated>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.eventFlow(EventThreadUpdated).collect { (_, event) ->
                events.add(event)
            }
        }

        proxyListener.onMessage(mockSocket, errorJson)

        assertTrue(events.isEmpty(), "eventFlow should not emit for non-matching event types")
        job.cancel()
    }

    @Test
    fun `eventFlow ignores malformed JSON`() = runTest {
        val events = mutableListOf<EventThreadUpdated>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.eventFlow(EventThreadUpdated).collect { (_, event) ->
                events.add(event)
            }
        }

        proxyListener.onMessage(mockSocket, "not valid json {{{")

        assertTrue(events.isEmpty(), "eventFlow should silently ignore malformed JSON")
        job.cancel()
    }

    @Test
    fun `eventFlow ignores messages that match type but fail full deserialization`() = runTest {
        // EventBlueprint will match ThreadUpdated type via postback.eventType,
        // but the full EventThreadUpdated deserialization will fail because
        // the data field is invalid (string instead of object).
        val malformedEvent = """{"postback":{"eventType":"ThreadUpdated","data":"invalid"}}"""

        val events = mutableListOf<EventThreadUpdated>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.eventFlow(EventThreadUpdated).collect { (_, event) ->
                events.add(event)
            }
        }

        proxyListener.onMessage(mockSocket, malformedEvent)

        assertTrue(events.isEmpty(), "eventFlow should silently ignore partial deserialization failures")
        job.cancel()
    }

    // -- awaitEventSuspend --

    @Test
    fun `awaitEventSuspend returns matching event`() = runTest {
        val threadId = UUID.randomUUID()
        val json = threadUpdatedJson(threadId)

        var result: EventThreadUpdated? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = proxyListener.awaitEventSuspend(EventThreadUpdated)
        }

        proxyListener.onMessage(mockSocket, json)
        job.join()

        assertEquals(threadId, result?.postback?.data?.threadId)
    }

    @Test
    fun `awaitEventSuspend respects filter`() = runTest {
        val targetId = UUID.randomUUID()
        val otherId = UUID.randomUUID()

        var result: EventThreadUpdated? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = proxyListener.awaitEventSuspend(EventThreadUpdated) {
                it.postback.data?.threadId == targetId
            }
        }

        // First message: does not match filter
        proxyListener.onMessage(mockSocket, threadUpdatedJson(otherId))
        // Second message: matches filter
        proxyListener.onMessage(mockSocket, threadUpdatedJson(targetId))
        job.join()

        assertEquals(targetId, result?.postback?.data?.threadId)
    }

    @Test
    fun `awaitEventSuspend is cancellable`() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.awaitEventSuspend(EventThreadUpdated)
        }

        assertTrue(job.isActive, "Job should be active while awaiting")
        job.cancel()
        assertTrue(job.isCancelled, "Job should be cancelled after cancel()")
    }

    // -- errorFlow --

    @Test
    fun `errorFlow emits for matching error type`() = runTest {
        val json = errorJson(ErrorType.SendingMessageFailed)

        val sockets = mutableListOf<WebSocket>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.errorFlow(ErrorType.SendingMessageFailed).collect { ws ->
                sockets.add(ws)
            }
        }

        proxyListener.onMessage(mockSocket, json)

        assertEquals(1, sockets.size)
        assertEquals(mockSocket, sockets[0])
        job.cancel()
    }

    @Test
    fun `errorFlow ignores non-matching error types`() = runTest {
        val json = errorJson(ErrorType.RecoveringThreadFailed)

        val sockets = mutableListOf<WebSocket>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.errorFlow(ErrorType.SendingMessageFailed).collect { ws ->
                sockets.add(ws)
            }
        }

        proxyListener.onMessage(mockSocket, json)

        assertTrue(sockets.isEmpty(), "errorFlow should not emit for non-matching error types")
        job.cancel()
    }

    @Test
    fun `errorFlow ignores non-error messages`() = runTest {
        val eventJson = threadUpdatedJson(UUID.randomUUID())

        val sockets = mutableListOf<WebSocket>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.errorFlow(ErrorType.SendingMessageFailed).collect { ws ->
                sockets.add(ws)
            }
        }

        proxyListener.onMessage(mockSocket, eventJson)

        assertTrue(sockets.isEmpty(), "errorFlow should not emit for non-error messages")
        job.cancel()
    }

    @Test
    fun `errorFlow ignores malformed JSON`() = runTest {
        val sockets = mutableListOf<WebSocket>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            proxyListener.errorFlow(ErrorType.SendingMessageFailed).collect { sockets.add(it) }
        }

        proxyListener.onMessage(mockSocket, "not json {{{")

        assertTrue(sockets.isEmpty(), "errorFlow should silently ignore malformed JSON")
        job.cancel()
    }

    // -- Test JSON helpers --

    private fun threadUpdatedJson(threadId: UUID): String =
        """{"postback":{"eventType":"ThreadUpdated","data":{"id":"$threadId"}}}"""

    private fun errorJson(errorType: ErrorType): String =
        """{"error":{"errorCode":"${errorType.value}","transactionId":"${UUID.randomUUID()}"}}"""
}
