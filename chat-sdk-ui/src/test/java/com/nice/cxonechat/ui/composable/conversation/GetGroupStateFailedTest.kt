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

package com.nice.cxonechat.ui.composable.conversation

import com.nice.cxonechat.message.MessageStatus.Delivered
import com.nice.cxonechat.message.MessageStatus.FailedToDeliver
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createSection
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createTestMessage
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.nextPerson
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetGroupStateFailedTest {
    @Test
    fun `Failed messages should always be marked as SOLO`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = FailedToDeliver, sender = person),
                createTestMessage(status = FailedToDeliver, sender = person),
            )
        )
        assertEquals(SOLO, getGroupState(section, 0))
        assertEquals(SOLO, getGroupState(section, 1))
    }

    @Test
    fun `Failed message should not affect grouping of normal messages that follow`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = FailedToDeliver, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }

    @Test
    fun `Isolated failed message should break the message group`() {
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered),
                createTestMessage(status = FailedToDeliver),
                createTestMessage(status = Delivered),
            )
        )
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
        assertEquals(LAST, getGroupState(section, 1))
        assertEquals(SOLO, getGroupState(section, 0))
    }

    @Test
    fun `Failed message in the middle of a conversation should create separate groups`() {
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered),
                createTestMessage(status = Delivered),
                createTestMessage(status = FailedToDeliver),
                createTestMessage(status = Delivered),
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, 1))
        assertEquals(LAST, getGroupState(section, 2))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }
}
