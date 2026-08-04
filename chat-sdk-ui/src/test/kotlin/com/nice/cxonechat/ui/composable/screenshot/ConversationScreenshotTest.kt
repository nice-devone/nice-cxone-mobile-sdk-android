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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageStatus.Delivered
import com.nice.cxonechat.message.MessageStatus.FailedToDeliver
import com.nice.cxonechat.message.MessageStatus.Read
import com.nice.cxonechat.message.MessageStatus.Seen
import com.nice.cxonechat.message.MessageStatus.Sending
import com.nice.cxonechat.message.MessageStatus.Sent
import com.nice.cxonechat.ui.composable.conversation.MessageGroupHeader
import com.nice.cxonechat.ui.composable.conversation.MessageStatusIndicator
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.TypingIndicatorMessage
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Person
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode
import com.nice.cxonechat.ui.util.preview.message.Text as MessageText

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-mdpi-finger")
@GraphicsMode(Mode.NATIVE)
class ConversationScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `text message received`() {
        composeTestRule.setContent {
            PreviewMessageItemBase(
                message = Message.Text(
                    MessageText(
                        text = "Hello! How can I help you today?",
                        direction = MessageDirection.ToClient,
                    )
                )
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `text message sent with long content`() {
        composeTestRule.setContent {
            PreviewMessageItemBase(
                message = Message.Text(
                    MessageText(
                        text = "Order #12345 was due last Monday but hasn't arrived yet.",
                        direction = MessageDirection.ToAgent,
                    )
                )
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `message status indicators all states`() {
        val statuses = listOf(Sending, Sent, Delivered, Seen, Read, FailedToDeliver)
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.width(220.dp)) {
                    Column {
                        for (status in statuses) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = status.name,
                                    style = ChatTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                MessageStatusIndicator(status = status)
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `typing indicator message`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        TypingIndicatorMessage(
                            agent = Person(firstName = "Support", lastName = "Agent"),
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `message group header`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    Column {
                        MessageGroupHeader(dayString = "Today")
                        MessageGroupHeader(dayString = "June 2, 2026")
                        MessageGroupHeader(dayString = "January 15, 2025")
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
