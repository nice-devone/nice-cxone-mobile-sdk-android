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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.domain.model.EndConversationChoice
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class EndConversationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsAgentNameAndMessage() {
        composeTestRule.setContent {
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                onUserSelection = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag("agent_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_title").assertIsDisplayed()
    }

    @Test
    fun hidesAgentNameIfNullOrEmpty() {
        composeTestRule.setContent {
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(null) },
                onUserSelection = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag("agent_name").assertDoesNotExist()
        composeTestRule.onNodeWithTag("top_title").assertIsDisplayed()
    }

    @Test
    fun actionButtonsTriggerCallbacks() {
        var selected: EndConversationChoice? = null
        var dismissed = false
        composeTestRule.setContent {
            val onDismiss = { dismissed = true }
            EndConversationBottomSheet(
                assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
                onUserSelection = { selected = it },
                onDismiss = onDismiss,
            )
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
}
