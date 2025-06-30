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

package com.nice.cxonechat.ui.composable.conversation

import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Delivered
import com.nice.cxonechat.message.MessageStatus.FailedToDeliver
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.tool.nextString
import com.nice.cxonechat.ui.util.preview.message.UiSdkMetadata
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Map.entry

/**
 * Tests for the [getGroupState] function which determines how messages should be visually
 * grouped in the conversation UI.
 *
 * The function analyzes message positioning, sender identity, and message status
 * to determine whether a message should appear as the first, middle, or last
 * message in a group, or if it should stand alone.
 */
internal class GetGroupStateTest {

    /**
     * Creates a new random [Person] instance for testing.
     *
     * @return A [Person] with randomly generated ID and name properties
     */
    private fun nextPerson() = Person(
        id = nextString(),
        firstName = nextString(),
        lastName = nextString(),
        imageUrl = null
    )

    /**
     * Creates a test message with the specified status, sender, and creation date.
     *
     * @param status The message status to set
     * @param sender The message sender, or null for default
     * @param createdAt When the message was created
     * @return A [Message.Text] instance with the specified properties
     */
    private fun createTestMessage(status: MessageStatus, sender: Person? = null, createdAt: Date = Date()): Message.Text {
        return Message.Text(
            UiSdkText(
                text = nextString(),
                metadata = UiSdkMetadata(status = status),
                author = sender,
                createdAt = createdAt
            )
        )
    }

    // -- Section basic grouping --
    @Test
    fun `Single message in a section should be marked as SOLO`() {
        val section = Section(
            entry(
                nextString(),
                listOf(createTestMessage(Delivered))
            )
        )
        val result = getGroupState(section, 0)
        assertEquals(SOLO, result)
    }

    @Test
    fun `Two messages from the same sender should form FIRST and LAST group`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }

    @Test
    fun `Three messages from the same sender should form FIRST MIDDLE and LAST group`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }

    // -- Section grouping with changing sender --
    @Test
    fun `Messages from different senders should be marked as SOLO`() {
        val person = nextPerson()
        val person2 = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person2),
                )
            )
        )
        assertEquals(SOLO, getGroupState(section, section.messages.lastIndex))
        assertEquals(SOLO, getGroupState(section, 0))
    }

    @Test
    fun `When the third message is from a different sender it should be marked as SOLO`() {
        val person = nextPerson()
        val person2 = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person2),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, 1))
        assertEquals(SOLO, getGroupState(section, section.messages.lastIndex))
    }

    @Test
    fun `Two messages from each sender should form two separate groups`() {
        val person = nextPerson()
        val person2 = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person2),
                    createTestMessage(status = Delivered, sender = person2),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, 1))
        assertEquals(LAST, getGroupState(section, 2))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }

    // -- Section grouping with changes in message status --
    @Test
    fun `Messages with different statuses should not be grouped together`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = MessageStatus.Seen, sender = person),
                )
            )
        )
        assertEquals(SOLO, getGroupState(section, 0))
        assertEquals(SOLO, getGroupState(section, 1))
    }

    @Test
    fun `Status changes should create separate message groups`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = MessageStatus.Seen, sender = person),
                    createTestMessage(status = MessageStatus.Seen, sender = person),
                    createTestMessage(status = MessageStatus.Seen, sender = person),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, 2))
        assertEquals(LAST, getGroupState(section, 3))
        assertEquals(MIDDLE, getGroupState(section, 4))
        assertEquals(FIRST, getGroupState(section, 5))
    }

    // -- Section grouping with FailedToDeliver messages --
    @Test
    fun `Failed messages should always be marked as SOLO`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = FailedToDeliver, sender = person),
                    createTestMessage(status = FailedToDeliver, sender = person),
                )
            )
        )
        assertEquals(SOLO, getGroupState(section, 0))
        assertEquals(SOLO, getGroupState(section, 1))
    }

    @Test
    fun `Failed message should not affect grouping of normal messages that follow`() {
        val person = nextPerson()
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = FailedToDeliver, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                    createTestMessage(status = Delivered, sender = person),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }

    @Test
    fun `Isolated failed message should break the message group`() {
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered),
                    createTestMessage(status = FailedToDeliver),
                    createTestMessage(status = Delivered)
                )
            )
        )
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
        assertEquals(LAST, getGroupState(section, 1))
        assertEquals(SOLO, getGroupState(section, 0))
    }

    @Test
    fun `Failed message in the middle of a conversation should create separate groups`() {
        val section = Section(
            entry(
                nextString(),
                listOf(
                    createTestMessage(status = Delivered),
                    createTestMessage(status = Delivered),
                    createTestMessage(status = FailedToDeliver),
                    createTestMessage(status = Delivered),
                )
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, 1))
        assertEquals(LAST, getGroupState(section, 2))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }
}
