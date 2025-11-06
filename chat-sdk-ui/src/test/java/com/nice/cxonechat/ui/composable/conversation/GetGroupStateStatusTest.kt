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
import com.nice.cxonechat.message.MessageStatus.Seen
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createSection
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.createTestMessage
import com.nice.cxonechat.ui.composable.conversation.GroupStateTestUtils.nextPerson
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetGroupStateStatusTest {
    @Test
    fun `Messages with different statuses should not be grouped together`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Seen, sender = person),
            )
        )
        assertEquals(SOLO, getGroupState(section, 0))
        assertEquals(SOLO, getGroupState(section, 1))
    }

    @Test
    fun `Status changes should create separate message groups`() {
        val person = nextPerson()
        val section = createSection(
            listOf(
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Delivered, sender = person),
                createTestMessage(status = Seen, sender = person),
                createTestMessage(status = Seen, sender = person),
                createTestMessage(status = Seen, sender = person),
            )
        )
        assertEquals(LAST, getGroupState(section, 0))
        assertEquals(MIDDLE, getGroupState(section, 1))
        assertEquals(FIRST, getGroupState(section, 2))
        assertEquals(LAST, getGroupState(section, 3))
        assertEquals(MIDDLE, getGroupState(section, 4))
        assertEquals(FIRST, getGroupState(section, 5))
    }
}
