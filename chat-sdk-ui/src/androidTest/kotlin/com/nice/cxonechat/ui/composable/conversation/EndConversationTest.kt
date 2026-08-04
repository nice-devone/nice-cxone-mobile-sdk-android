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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.EndConversationChoice
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndConversationTest : AbstractComponentActivityUiTest() {

    @Test
    fun showsAgentNameAndMessage() {
        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                    liveChatAllowTranscript = true,
                    onUserSelection = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule
            .onNodeWithTag("agent_name", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_title", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun hidesAgentNameIfNullOrEmpty() {
        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(null) },
                    liveChatAllowTranscript = true,
                    onUserSelection = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("agent_name", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("top_title").assertIsDisplayed()
    }

    @Test
    fun actionButtonsTriggerCallbacks() {
        var selected: EndConversationChoice? = null
        var dismissed = false
        composeTestRule.setContent {
            ChatTheme {
                val onDismiss = { dismissed = true }
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                    liveChatAllowTranscript = true,
                    onUserSelection = { selected = it },
                    onDismiss = onDismiss,
                )
            }
        }
        composeTestRule.onNodeWithTag("start_new_chat_button").performClick()
        Assert.assertEquals(EndConversationChoice.NEW_CONVERSATION, selected)
        Assert.assertTrue(dismissed)
        dismissed = false
        composeTestRule.onNodeWithTag("back_to_conversation_button").performClick()
        Assert.assertEquals(EndConversationChoice.SHOW_TRANSCRIPT, selected)
        Assert.assertTrue(dismissed)
        dismissed = false
        composeTestRule.onNodeWithTag("close_chat_button").performClick()
        Assert.assertEquals(EndConversationChoice.CLOSE_CHAT, selected)
        Assert.assertTrue(dismissed)
    }

    @Test
    fun sendTranscriptButton_hidden_whenLiveChatAllowTranscriptFalse() {
        composeTestRule.setContent {
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                liveChatAllowTranscript = false,
                onUserSelection = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag("send_transcript_button").assertDoesNotExist()
    }

    @Test
    fun sendTranscriptButton_shown_whenLiveChatAllowTranscriptTrue() {
        composeTestRule.setContent {
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                liveChatAllowTranscript = true,
                onUserSelection = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag("send_transcript_button").assertIsDisplayed()
    }

    @Test
    fun sendTranscriptButton_triggersCallback_whenClicked() {
        var selected: EndConversationChoice? = null
        composeTestRule.setContent {
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                liveChatAllowTranscript = true,
                onUserSelection = { selected = it },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag("send_transcript_button").performClick()
        Assert.assertEquals(EndConversationChoice.SEND_TRANSCRIPT, selected)
    }

    /**
     * Test that the bottom sheet is displayed with proper accessibility.
     */
    @Test
    fun endConversationBottomSheet_isDisplayedWithAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("end_conversation_bottom_sheet")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that all action buttons are accessible and clickable with agent.
     */
    @Test
    fun endConversationBottomSheet_allActionsAccessible_withAgent() {
        var selectedChoice: EndConversationChoice? = null
        val agent = PreviewAgent.nextAgent()

        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(agent) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = { selectedChoice = it }
                )
            }
        }

        // Verify title is accessible (note: text is merged with agent name in semantics)
        composeTestRule.onNodeWithTag("top_title", useUnmergedTree = true)
            .assertExists()
            .tryPerformAccessibilityChecks()

        // Verify agent card is accessible
        composeTestRule.onNodeWithTag("agent_card", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify the semantic grouping exists (title + agent name)
        val closeMessage = getString(R.string.livechat_conversation_closed_message)
        val expectedDescription = "$closeMessage ${agent.fullName}"
        composeTestRule.onNodeWithContentDescription(expectedDescription)
            .assertExists()
            .tryPerformAccessibilityChecks()

        // Test "Start new chat" button
        composeTestRule.onNodeWithTag("start_new_chat_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .performClick()

        Assert.assertEquals(EndConversationChoice.NEW_CONVERSATION, selectedChoice)

        // Reset for next test
        selectedChoice = null

        // Test "Back to conversation" button
        composeTestRule.onNodeWithTag("back_to_conversation_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .performClick()

        Assert.assertEquals(EndConversationChoice.SHOW_TRANSCRIPT, selectedChoice)

        // Reset for next test
        selectedChoice = null

        // Test "Send transcript" button
        composeTestRule.onNodeWithTag("send_transcript_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .performClick()

        Assert.assertEquals(EndConversationChoice.SEND_TRANSCRIPT, selectedChoice)

        // Reset for next test
        selectedChoice = null

        // Test "Close chat" button
        composeTestRule.onNodeWithTag("close_chat_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .performClick()

        Assert.assertEquals(EndConversationChoice.CLOSE_CHAT, selectedChoice)
    }

    /**
     * Test that all action buttons are accessible without agent.
     */
    @Test
    fun endConversationBottomSheet_allActionsAccessible_withoutAgent() {
        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(null) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = {}
                )
            }
        }

        // Verify title message is accessible (text should be directly searchable without agent)
        composeTestRule.onNodeWithTag("top_title")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify the content description exists for screen readers
        val closeMessageNoAgent = getString(R.string.livechat_conversation_closed_message_no_agent)
        composeTestRule.onNodeWithContentDescription(closeMessageNoAgent)
            .assertExists()
            .tryPerformAccessibilityChecks()

        // Verify agent card is not displayed
        composeTestRule.onNodeWithTag("agent_card")
            .assertDoesNotExist()

        // Verify all action buttons are still accessible
        composeTestRule.onNodeWithTag("start_new_chat_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("back_to_conversation_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("send_transcript_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("close_chat_button")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that button texts are properly labeled for accessibility.
     */
    @Test
    fun endConversationBottomSheet_buttonTextsAreAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = {}
                )
            }
        }

        // Verify all button texts are displayed and accessible
        val newChatText = getString(R.string.livechat_new_chat)
        composeTestRule.onNodeWithText(newChatText)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        val showTranscriptText = getString(R.string.livechat_show_transcript)
        composeTestRule.onNodeWithText(showTranscriptText)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        val sendTranscriptText = getString(R.string.send_transcript)
        composeTestRule.onNodeWithText(sendTranscriptText)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        val closeChatText = getString(R.string.livechat_close_chat)
        composeTestRule.onNodeWithText(closeChatText)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the agent name is displayed and accessible.
     */
    @Test
    fun endConversationBottomSheet_agentNameIsAccessible() {
        val agent = PreviewAgent.nextAgent()

        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(agent) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = {}
                )
            }
        }

        // Verify agent name is accessible via test tag
        composeTestRule.onNodeWithTag("agent_name", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test semantic grouping for screen readers.
     */
    @Test
    fun endConversationBottomSheet_semanticGroupingWithAgent() {
        val agent = PreviewAgent.nextAgent()
        val closeMessage = getString(R.string.livechat_conversation_closed_message)
        val expectedDescription = "$closeMessage ${agent.fullName}"

        composeTestRule.setContent {
            ChatTheme {
                EndConversationBottomSheet(
                    assignedAgent = remember { mutableStateOf(agent) },
                    onDismiss = {},
                    liveChatAllowTranscript = true,
                    onUserSelection = {}
                )
            }
        }

        // Verify that the title and agent name are semantically grouped
        composeTestRule.onNodeWithContentDescription(expectedDescription)
            .assertExists()
            .tryPerformAccessibilityChecks()
    }
}
