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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage
import org.junit.Test

class UnsupportedMessageTest : AbstractComponentActivityUiTest() {

    @Test
    fun unsupportedMessage_displaysFallbackTextAndStatus() {
        composeTestRule.setContent {
            PreviewMessageItemBase {
                PreviewMessageItem(
                    message = Message.Unsupported(UiSdkUnsupportedMessage(direction = ToAgent)),
                    showStatus = DisplayStatus.DISPLAY,
                    messageStatusState = MessageStatusState.DISABLED,
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {},
                )
            }
        }

        // Check fallback text node is displayed
        composeTestRule.onNodeWithTag(TAG_TEXT, useUnmergedTree = true).assertIsDisplayed()

        // Check status text is displayed
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.unsupported_message_status)
        ).assertIsDisplayed()

        // Check status indicator is displayed
        composeTestRule.onNodeWithTag("message_status_indicator", true)
            .assertIsDisplayed()
    }

    @Test
    fun unsupportedMessageStatus_displaysIconAndText_andShowsTooltipOnClick() {
        composeTestRule.setContent {
            Surface {
                PreviewMessageItem(message = Message.Unsupported(UiSdkUnsupportedMessage()))
            }
        }

        val tooltipText = getString(R.string.unsupported_message)
        val messageBody = composeTestRule.onNodeWithTag(TAG_BODY).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.unsupported_message_status)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(tooltipText).assertDoesNotExist()

        messageBody.performClick()

        // The tooltip animates in its own window, so wait for it to appear, not for idle.
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithText(tooltipText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(tooltipText).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun fallbackText_fromUser_hasAccessibilityDescription() {
        val testText = "Test unsupported content"
        val message = Message.Unsupported(
            UiSdkUnsupportedMessage(
                direction = ToAgent,
                author = null,
                text = testText,
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                UnsupportedMessage(message = message, modifier = Modifier)
            }
        }

        val expectedDescription = getString(
            R.string.accessibility_message_you,
            testText,
            message.createdAtDate(composeTestRule.activity.resources),
        )
        val node = composeTestRule.onNodeWithTag(TAG_BODY)
        node.assertExists()
        node.assertContentDescriptionContains(value = expectedDescription)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun fallbackText_fromAgent_hasAccessibilityDescription() {
        val testText = "Test unsupported agent content"
        val agentFirstName = "Agent"
        val agentLastName = "Smith"
        val message = Message.Unsupported(
            UiSdkUnsupportedMessage(
                direction = ToClient,
                text = testText,
                author = object : MessageAuthor() {
                    override val id = "agent1"
                    override val firstName = agentFirstName
                    override val lastName = agentLastName
                    override val imageUrl: String? = null
                }
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                UnsupportedMessage(message = message, modifier = Modifier)
            }
        }

        val expectedDescription = getString(
            R.string.accessibility_message_from_agent,
            "$agentFirstName $agentLastName",
            testText,
            message.createdAtDate(composeTestRule.activity.resources),
        )
        val node = composeTestRule.onNodeWithTag(TAG_BODY)
        node.assertExists()
        node.assertContentDescriptionContains(value = expectedDescription)
    }

    companion object {
        private const val TAG_TEXT = "fallback_text"
        private const val TAG_BODY = "unsupported_message"
    }
}
