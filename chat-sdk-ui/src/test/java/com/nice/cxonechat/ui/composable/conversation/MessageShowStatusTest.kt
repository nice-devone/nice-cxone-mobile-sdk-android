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

import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO_GROUPED
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.util.preview.message.UiSdkMetadata
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Date

/**
 * Unit tests for [Message.showStatus].
 */
internal class MessageShowStatusTest {

    private fun textMessage(
        direction: MessageDirection = MessageDirection.ToAgent,
        status: MessageStatus = MessageStatus.Delivered,
    ) = Message.Text(
        UiSdkText(
            text = "test",
            metadata = UiSdkMetadata(status = status),
            author = null,
            createdAt = Date(),
            direction = direction
        )
    )

    @Test
    fun `returns HIDE for ToClient direction regardless of other params`() {
        val message = textMessage(direction = MessageDirection.ToClient)
        MessageItemGroupState.entries.forEach { groupState ->
            assertEquals(DisplayStatus.HIDE, message.showStatus(groupState, isLastMessage = true))
            assertEquals(DisplayStatus.HIDE, message.showStatus(groupState, isLastMessage = false))
        }
    }

    @Test
    fun `returns DISPLAY for FailedToDeliver status regardless of other params`() {
        val message = textMessage(status = MessageStatus.FailedToDeliver)
        MessageItemGroupState.entries.forEach { groupState ->
            assertEquals(DisplayStatus.DISPLAY, message.showStatus(groupState, isLastMessage = true))
            assertEquals(DisplayStatus.DISPLAY, message.showStatus(groupState, isLastMessage = false))
        }
    }

    @Test
    fun `returns DISPLAY for last message in group with LAST, SOLO, SOLO_GROUPED`() {
        val message = textMessage()
        listOf(LAST, SOLO, SOLO_GROUPED).forEach { groupState ->
            assertEquals(DisplayStatus.DISPLAY, message.showStatus(groupState, isLastMessage = true))
            assertNotEquals(DisplayStatus.DISPLAY, message.showStatus(groupState, isLastMessage = false))
        }
    }

    @Test
    fun `returns SPACER for non-last message with LAST, SOLO`() {
        val message = textMessage()
        listOf(LAST, SOLO).forEach { groupState ->
            assertEquals(DisplayStatus.SPACER, message.showStatus(groupState, isLastMessage = false))
        }
    }

    @Test
    fun `returns HIDE for all other cases`() {
        val message = textMessage()
        val covered = setOf(LAST, SOLO, SOLO_GROUPED)
        MessageItemGroupState.entries.filterNot { it in covered }.forEach { groupState ->
            assertEquals(DisplayStatus.HIDE, message.showStatus(groupState, isLastMessage = false))
            assertEquals(DisplayStatus.HIDE, message.showStatus(groupState, isLastMessage = true))
        }
    }
}
