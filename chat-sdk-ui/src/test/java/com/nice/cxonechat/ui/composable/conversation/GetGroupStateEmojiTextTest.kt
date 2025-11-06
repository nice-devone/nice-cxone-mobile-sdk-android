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

import com.nice.cxonechat.message.MessageStatus.Delivered
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createSection
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createTestEmojiMessage
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createTestMessage
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.nextPerson
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST_SQUASHED
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO_GROUPED
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetGroupStateEmojiTextTest {
    @Test
    fun `Text message group before EmojiText should be marked as LAST, MIDDLE & FIRST`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestEmojiMessage(status = Delivered, sender = person),
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, 2))
    }

    @Test
    fun `Text message group after EmojiText should be marked as LAST_SQUASHED, MIDDLE & FIRST`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestEmojiMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
            )
        )
        assertEquals(LAST_SQUASHED, getGroupState(section, 1))
        assertEquals(MIDDLE, getGroupState(section, 2))
        assertEquals(FIRST, getGroupState(section, 3))
    }

    @Test
    fun `Text message before EmojiText should be marked as SOLO`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestEmojiMessage(status = Delivered, sender = person)
            )
        )
        assertEquals(SOLO, getGroupState(section, 0))
    }

    @Test
    fun `Text message after EmojiText should be marked as SOLO_GROUPED`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestEmojiMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
            )
        )
        assertEquals(SOLO_GROUPED, getGroupState(section, 1))
    }

    @Test
    fun `Text message between two EmojiText messages should be marked as SOLO_GROUPED`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestEmojiMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestEmojiMessage(status = Delivered, sender = person),
            )
        )
        assertEquals(SOLO_GROUPED, getGroupState(section, 1))
    }
}
