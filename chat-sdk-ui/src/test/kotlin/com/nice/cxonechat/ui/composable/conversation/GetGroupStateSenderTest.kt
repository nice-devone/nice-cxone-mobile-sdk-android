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
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createSection
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createTestMessage
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.nextPerson
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetGroupStateSenderTest {
    @Test
    fun `Messages from different senders should be marked as SOLO`() {
        val person = nextPerson()
        val person2 = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person2),
            )
        )
        assertEquals(SOLO, getGroupState(section, section.messages.lastIndex))
        assertEquals(SOLO, getGroupState(section, 0))
    }

    @Test
    fun `When the third message is from a different sender it should be marked as SOLO`() {
        val person = nextPerson()
        val person2 = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person2),
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
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person2),
                createTestMessage(status = Delivered, sender = person2),
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(FIRST, getGroupState(section, 1))
        assertEquals(LAST, getGroupState(section, 2))
        assertEquals(FIRST, getGroupState(section, section.messages.lastIndex))
    }
}
