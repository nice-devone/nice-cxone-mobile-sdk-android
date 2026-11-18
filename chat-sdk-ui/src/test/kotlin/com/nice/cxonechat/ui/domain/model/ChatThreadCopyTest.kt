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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ContactHandler
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the `ChatThreadCopy.Companion.copy` extension function (imported as `ChatThread.copy()`).
 *
 * [ChatThread.contactId] was promoted to a public `abstract val` in DE-171902. The
 * `copy()` extension must correctly preserve or override `contactId` (and every other
 * property) so that nothing is silently dropped during a partial update.
 */
class ChatThreadCopyTest {

    @Test
    fun `copy preserves contactId from original thread when not overridden`() {
        val original = makeThread(contactId = "contact-xyz")

        val copied = original.copy()

        assertEquals("contact-xyz", copied.contactId)
    }

    @Test
    fun `copy replaces contactId when an explicit value is provided`() {
        val original = makeThread(contactId = "original-contact")

        val copied = original.copy(contactId = "new-contact")

        assertEquals("new-contact", copied.contactId)
    }

    @Test
    fun `copy sets contactId to null when null is explicitly provided`() {
        val original = makeThread(contactId = "some-contact")

        val copied = original.copy(contactId = null)

        assertNull(copied.contactId)
    }

    @Test
    fun `copy preserves null contactId from original thread`() {
        val original = makeThread(contactId = null)

        val copied = original.copy()

        assertNull(copied.contactId)
    }

    @Test
    fun `copy produces a ChatThreadCopy with identical properties when no overrides given`() {
        val id = UUID.randomUUID()
        val original = makeThread(
            id = id,
            threadName = "My Thread",
            contactId = "contact-abc",
            scrollToken = "scroll-1",
            canAddMoreMessages = false,
            threadState = ChatThreadState.Closed,
            positionInQueue = 3,
            hasOnlineAgent = false,
        )

        val copied = original.copy()

        assertEquals(id, copied.id)
        assertEquals("My Thread", copied.threadName)
        assertEquals("contact-abc", copied.contactId)
        assertEquals("scroll-1", copied.scrollToken)
        assertEquals(emptyList<Message>(), copied.messages)
        assertEquals(null, copied.threadAgent)
        assertEquals(false, copied.canAddMoreMessages)
        assertEquals(ChatThreadState.Closed, copied.threadState)
        assertEquals(emptyList<CustomField>(), copied.fields)
        assertEquals(3, copied.positionInQueue)
        assertEquals(false, copied.hasOnlineAgent)
    }

    @Test
    fun `copy preserves contactHandler from original thread when not overridden`() {
        val original = makeThread(contactHandler = ContactHandler.Cognigy)

        val copied = original.copy()

        assertEquals(ContactHandler.Cognigy, copied.contactHandler)
    }

    // ---

    @Suppress("LongParameterList")
    private fun makeThread(
        id: UUID = UUID.randomUUID(),
        threadName: String? = null,
        contactId: String? = null,
        scrollToken: String = "",
        canAddMoreMessages: Boolean = true,
        threadState: ChatThreadState = ChatThreadState.Ready,
        positionInQueue: Int? = null,
        hasOnlineAgent: Boolean = true,
        contactHandler: ContactHandler? = null,
    ): ChatThread = object : ChatThread() {
        override val id: UUID = id
        override val threadName: String? = threadName
        override val messages: List<Message> = emptyList()
        override val threadAgent: Agent? = null
        override val canAddMoreMessages: Boolean = canAddMoreMessages
        override val scrollToken: String = scrollToken
        override val threadState: ChatThreadState = threadState
        override val fields: List<CustomField> = emptyList()
        override val positionInQueue: Int? = positionInQueue
        override val hasOnlineAgent: Boolean = hasOnlineAgent
        override val contactId: String? = contactId
        override val contactHandler: ContactHandler? = contactHandler
    }
}
