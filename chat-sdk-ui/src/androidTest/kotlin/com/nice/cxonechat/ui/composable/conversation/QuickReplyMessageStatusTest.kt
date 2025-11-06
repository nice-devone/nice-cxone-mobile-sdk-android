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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply
import org.junit.Rule
import org.junit.Test

class QuickReplyMessageStatusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsSelectedState() {
        composeTestRule.setContent {
            QuickReplyMessageStatus(MessageStatusState.SELECTED, onClick = {})
        }
        composeTestRule.onNodeWithTag("quick_reply_message_status")
            .assertExists()
            .assertTextContains("option selected", ignoreCase = true)
    }

    @Test
    fun showsDisabledState_andTriggersOnClick() {
        var clicked = false
        composeTestRule.setContent {
            QuickReplyMessageStatus(MessageStatusState.DISABLED, onClick = { clicked = true })
        }
        composeTestRule.onNodeWithTag("quick_reply_message_status")
            .assertExists()
            .assertTextContains("Options unavailable", ignoreCase = true)
            .performClick()
        assert(clicked)
    }

    @Test
    fun showsSelectableState() {
        composeTestRule.setContent {
            QuickReplyMessageStatus(MessageStatusState.SELECTABLE, onClick = {})
        }
        composeTestRule.onNodeWithTag("quick_reply_message_status")
            .assertExists()
            .assertTextContains("Select one option below", ignoreCase = true)
    }


    @Test
    fun quickReplyOptions_displayAndSelect() {
        var selectedCalled = false

        composeTestRule.setContent {
            QuickReplyOptions(
                message = QuickReply(UiSdkQuickReply()) {},
                onOptionSelected = { selectedCalled = true }
            )
        }

        // Check options are displayed
        composeTestRule.onNodeWithText("Some text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Random cat").assertIsDisplayed()

        // Click on an option
        composeTestRule.onNodeWithText("Some text").performClick()

        // Verify callback was called
        assert(selectedCalled)
    }
}
