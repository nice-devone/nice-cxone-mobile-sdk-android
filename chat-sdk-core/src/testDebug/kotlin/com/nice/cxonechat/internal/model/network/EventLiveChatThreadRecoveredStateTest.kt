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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.toReceived
import com.nice.cxonechat.thread.ChatThreadState
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Unit tests for [EventLiveChatThreadRecovered.threadState] and [EventLiveChatThreadRecovered.inThread].
 *
 * **Why these matter:**
 * `threadState` is the gate that determines whether a recovered LiveChat thread is ready to accept
 * new messages (`Ready`), just loaded (`Loaded`), or permanently closed (`Closed`).  Getting it
 * wrong causes the chat UI to show an open input box on a closed thread, or hide it on a ready one.
 *
 * `inThread` guards which threads are notified when a recovery event arrives.  The edge case where
 * `this.thread` is null (the server sent no thread data at all) or a message's `threadId` doesn't
 * match the recovered thread would silently route events to the wrong handler.
 */
internal class EventLiveChatThreadRecoveredStateTest {

    // ---- helpers ----

    private fun event(data: EventLiveChatThreadRecovered.Data) =
        EventLiveChatThreadRecovered(
            postback = Postback(eventType = EventType.LivechatRecovered, data = data)
        )

    private fun contactField(
        id: String = "contact-1",
        status: ContactStatus = ContactStatus.New,
        threadIdOnExternalPlatform: UUID = UUID.randomUUID(),
    ) = ContactFieldData(
        id = id,
        customFields = emptyList(),
        threadIdOnExternalPlatform = threadIdOnExternalPlatform,
        status = status,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    // ---- threadState tests ----

    @Test
    fun `threadState is Closed when contact status is Closed`() {
        val thread = makeChatThread()
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            contact = contactField(status = ContactStatus.Closed),
        )
        assertEquals(ChatThreadState.Closed, event(data).threadState)
    }

    @Test
    fun `threadState is Ready when thread data and contactId are both present and not Closed`() {
        val thread = makeChatThread()
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            contact = contactField(status = ContactStatus.Open),
        )
        assertEquals(ChatThreadState.Ready, event(data).threadState)
    }

    @Test
    fun `threadState is Loaded when thread data is null`() {
        // Server sent a recovery event that carries no thread object at all — contact may be
        // present but without a thread we cannot have a Ready state.
        val data = EventLiveChatThreadRecovered.Data(
            thread = null,
            messagesScrollToken = "",
            contact = contactField(status = ContactStatus.New),
        )
        assertEquals(ChatThreadState.Loaded, event(data).threadState)
    }

    @Test
    fun `threadState is Loaded when contact is null even if thread data is present`() {
        // Thread data is present but the contact field is absent — contactId resolves to null,
        // so the thread cannot be Ready.
        val thread = makeChatThread()
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            contact = null,
        )
        assertEquals(ChatThreadState.Loaded, event(data).threadState)
    }

    // ---- inThread tests ----

    @Test
    fun `inThread returns false when event has no thread data`() {
        val data = EventLiveChatThreadRecovered.Data(
            thread = null,
            messagesScrollToken = "",
        )
        val thread = makeChatThread()
        assertFalse(event(data).inThread(thread))
    }

    @Test
    fun `inThread returns false when event thread id does not match the supplied thread`() {
        val eventThread = makeChatThread()
        val otherThread = makeChatThread()
        val data = EventLiveChatThreadRecovered.Data(
            thread = eventThread.toReceived(),
            messagesScrollToken = "",
        )
        assertFalse(event(data).inThread(otherThread))
    }

    @Test
    fun `inThread returns true when thread id matches and all messages belong to that thread`() {
        val thread = makeChatThread()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            messages = listOf(message),
        )
        assertTrue(event(data).inThread(thread))
    }

    @Test
    fun `inThread returns false when a message belongs to a different thread`() {
        // A message arriving in the payload with a different threadId than the recovered thread
        // must cause inThread to return false — prevents cross-thread event routing.
        val thread = makeChatThread()
        val foreignMessage = makeMessageModel(threadIdOnExternalPlatform = UUID.randomUUID())
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            messages = listOf(foreignMessage),
        )
        assertFalse(event(data).inThread(thread))
    }

    @Test
    fun `inThread ignores a foreign Postback message because messages drops non-mappable content`() {
        // Documents a real hole rather than asserting a desirable outcome: `messages` is
        // `data.messages.orEmpty().mapNotNull(MessageModel::toMessage)`, and toMessage returns null
        // for Postback and Noop content (see MessageModel.toMessage). Such an entry therefore never reaches
        // the `all {}` guard, so a foreign threadId on a Postback message does NOT make inThread
        // false — unlike the mappable case in the test above.
        val thread = makeChatThread()
        val foreignPostback = makeMessageModel(
            threadIdOnExternalPlatform = UUID.randomUUID(),
            messageContent = MessagePolyContent.Postback(postback = "postback-value"),
        )
        val data = EventLiveChatThreadRecovered.Data(
            thread = thread.toReceived(),
            messagesScrollToken = "",
            messages = listOf(foreignPostback),
        )

        assertTrue(
            event(data).inThread(thread),
            "Postback content is dropped by mapNotNull, so it cannot fail the all {} guard",
        )
    }
}
