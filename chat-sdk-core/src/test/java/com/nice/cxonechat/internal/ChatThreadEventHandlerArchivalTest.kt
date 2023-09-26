/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.ChatThreadEventHandlerActions.archiveThread
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingStart
import com.nice.cxonechat.enums.EventType.ThreadUpdated
import com.nice.cxonechat.event.thread.ArchiveThreadEvent
import com.nice.cxonechat.event.thread.TypingStartEvent
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.model.network.Postback
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import io.mockk.Ordering.ORDERED
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.WebSocket
import org.junit.Before
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatThreadEventHandlerArchivalTest {
    private val threadId = UUID.randomUUID()
    private val thread = ChatThreadMutable.from(ChatThreadInternal(threadId))
    private lateinit var socketListener: ProxyWebSocketListener
    private lateinit var origin: ChatThreadEventHandler
    private lateinit var chat: ChatWithParameters
    private lateinit var handler: ChatThreadEventHandlerArchival
    private lateinit var socket: WebSocket

    @Before
    fun setUp() {
        socket = mockk()
        socketListener = mockk {
            every { onMessage(any(), any<String>()) } returns Unit
        }
        origin = mockk {
            every { trigger(any(), any()) } answers {
                (it.invocation.args[1] as? OnEventSentListener)?.onSent()
            }
        }
        chat = mockk {
            every { socketListener } returns this@ChatThreadEventHandlerArchivalTest.socketListener
            every { socket } returns this@ChatThreadEventHandlerArchivalTest.socket
        }
        handler = ChatThreadEventHandlerArchival(origin, chat, thread)
    }

    @Test
    fun ignoresNonArchivalEvents() {
        val onSent = mockk<OnEventSentListener>(relaxed = true)

        handler.typingStart(onSent)

        verify {
            origin.trigger(any<TypingStartEvent>(), any())
            onSent.onSent()
        }

        assertTrue(thread.canAddMoreMessages)

        verify(exactly = 0) {
            socketListener.onMessage(any(), any<String>())
        }
    }

    @Test
    fun archivalEvents() {
        val onSent = mockk<OnEventSentListener>(relaxed = true)

        handler.archiveThread(onSent)

        val expect = EventThreadUpdated(
            Postback(
                ThreadUpdated,
                EventThreadUpdated.Data(
                    threadId
                )
            )
        ).let<EventThreadUpdated, String>(Default.serializer::toJson)

        verify(ordering = ORDERED) {
            origin.trigger(any<ArchiveThreadEvent>(), any())
            socketListener.onMessage(socket, expect)
            onSent.onSent()
        }

        assertFalse(thread.canAddMoreMessages)
    }
}
