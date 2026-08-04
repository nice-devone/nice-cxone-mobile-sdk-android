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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.event.AnalyticsEvent.Data.ValueMapData
import com.nice.cxonechat.event.AnalyticsEvent.Destination
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.PageViewEndedEvent
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import okhttp3.WebSocket
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Clock

internal class ChatEventHandlerImplTest {

    private val mockConnection = mockk<Connection>(relaxed = true) {
        every { brandId } returns 42
    }
    private val mockStorage = mockk<ValueStorage>(relaxed = true) {
        every { visitorId } returns UUID.randomUUID()
    }
    private val mockService = mockk<RemoteService>()
    private val mockSocket = mockk<WebSocket>()
    private val mockChatStateListener = mockk<ChatStateListener>(relaxed = true)
    private val mockChat = mockk<ChatWithParameters> {
        every { connection } returns mockConnection
        every { storage } returns mockStorage
        every { service } returns mockService
        every { socket } returns mockSocket
        every { entrails } returns mockk(relaxed = true)
        every { chatStateListener } returns mockChatStateListener
    }

    private val handler = ChatEventHandlerImpl(mockChat)

    @Test
    fun `trigger returns immediately for LocalEvent input`() = runTest {
        val localEvent = PageViewEndedEvent("title", "https://example.com")

        handler.trigger(localEvent)

        verify(exactly = 0) { mockService.postEvent(any(), any(), any()) }
        verify(exactly = 0) { mockSocket.send(text = any()) }
    }

    @Test
    fun `trigger posts analytics event via Retrofit on success`() = runTest {
        val analyticsEvent = makeAnalyticsEvent()
        val event = ChatEvent.Custom { _, _ -> analyticsEvent }
        val mockCall = mockk<Call<Void>> {
            every { enqueue(any()) } answers {
                val callback = arg<Callback<Void>>(0)
                callback.onResponse(this@mockk, Response.success(null))
            }
            every { cancel() } returns Unit
        }
        every { mockService.postEvent(any(), any(), any()) } returns mockCall

        handler.trigger(event)

        verify { mockService.postEvent(any(), any(), analyticsEvent) }
    }

    @Test
    fun `trigger does not throw on analytics Retrofit failure and does not surface it to the state listener`() = runTest {
        val analyticsEvent = makeAnalyticsEvent()
        val event = ChatEvent.Custom { _, _ -> analyticsEvent }
        val mockCall = mockk<Call<Void>> {
            every { enqueue(any()) } answers {
                val callback = arg<Callback<Void>>(0)
                callback.onFailure(this@mockk, RuntimeException("network error"))
            }
            every { cancel() } returns Unit
        }
        every { mockService.postEvent(any(), any(), any()) } returns mockCall

        // Analytics dispatch is fire-and-forget telemetry: a network failure must complete normally
        // (no throw — otherwise an uncaught exception on the caller's scope crashes the app) and is
        // logged only. It must NOT be routed to onChatRuntimeException: that handler overwrites the
        // current error state, so a high-frequency analytics failure would clear a real error dialog.
        handler.trigger(event)

        verify(exactly = 0) { mockChatStateListener.onChatRuntimeException(any()) }
    }

    @Test
    fun `trigger sends model via WebSocket for non-analytics event`() = runTest {
        val model = TestWsModel()
        val event = ChatEvent.Custom { _, _ -> model }
        every { mockSocket.send(text = any()) } returns true

        handler.trigger(event)

        verify { mockSocket.send(text = any()) }
    }

    @Test
    fun `trigger rethrows CXoneException from getModel`() = runTest {
        val event = ChatEvent.Custom { _, _ -> throw InvalidStateException("test") }

        assertFailsWith<InvalidStateException> {
            handler.trigger(event)
        }
    }

    @Test
    fun `trigger wraps ParseException in InternalError`() = runTest {
        val event = ChatEvent.Custom { _, _ -> throw ParseException("bad format", 0) }

        assertFailsWith<InternalError> {
            handler.trigger(event)
        }
    }

    @Test
    fun `trigger wraps SerializationException in InternalError`() = runTest {
        val event = ChatEvent.Custom { _, _ -> throw SerializationException("bad data") }

        assertFailsWith<InternalError> {
            handler.trigger(event)
        }
    }

    private fun makeAnalyticsEvent() = AnalyticsEvent(
        eventId = UUID.randomUUID(),
        type = VisitorEventType.VisitorVisit,
        visitId = UUID.randomUUID(),
        destinationId = Destination(UUID.randomUUID()),
        createdAt = Clock.System.now(),
        data = ValueMapData(emptyMap()),
    )

    @Serializable
    private data class TestWsModel(val value: String = "test")
}
