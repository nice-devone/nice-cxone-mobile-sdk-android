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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Test

/**
 * Tests for [QuickReplyMessage] status display and interaction handling.
 *
 * Tests verify:
 * - Status displays correct text for SELECTED, DISABLED, and SELECTABLE states
 * - Click handling for DISABLED state shows snackbar via parent component
 * - Quick reply options are displayed and selectable
 * - Parent component handles accessibility interactions (post-refactoring architecture)
 */
class QuickReplyMessageStatusTest : AbstractComponentActivityUiTest() {

    @Test
    fun showsSelectedState() {
        composeTestRule.setContent {
            PreviewMessageItemBase {
                QuickReplyMessageStatus(MessageStatusState.SELECTED)
            }
        }
        composeTestRule.onNodeWithTag("quick_reply_message_status")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_reply_message_status_text", useUnmergedTree = true)
            .assertExists()
            .assertTextContains(getString(R.string.option_selected))
            .assertIsDisplayed()
    }

    @Test
    fun showsDisabledState_andTriggersOnClick() {
        // Setup: Mock SnackbarHostState to verify click handling
        val snackBarHostState = mockk<SnackbarHostState> {
            coEvery { showSnackbar(any(), any(), any(), any()) } returns mockk()
        }

        composeTestRule.setContent {
            PreviewMessageItemBase {
                // Test parent QuickReplyMessage component (not just status)
                // After refactoring, parent handles click interactions for accessibility
                QuickReplyMessage(
                    message = QuickReply(UiSdkQuickReply()) {},
                    messageStatusState = MessageStatusState.DISABLED,
                    snackBarHostState = snackBarHostState
                )
            }
        }

        // Verify: Disabled state text is displayed in the status component
        composeTestRule.onNodeWithTag("quick_reply_message_status_text", useUnmergedTree = true)
            .assertExists()
            .assertTextContains(getString(R.string.options_unavailable))

        // Action: Click on the parent message (which handles clicks when DISABLED)
        composeTestRule.onNodeWithTag("quick_reply_message").performClick()

        // Verify: Snackbar was triggered with detailed disable message
        coVerify {
            snackBarHostState.showSnackbar(
                message = any(),
                duration = any(),
                withDismissAction = any(),
                actionLabel = any()
            )
        }
    }

    @Test
    fun showsSelectableState() {
        composeTestRule.setContent {
            PreviewMessageItemBase {
                QuickReplyMessageStatus(MessageStatusState.SELECTABLE)
            }
        }
        composeTestRule.onNodeWithTag("quick_reply_message_status_text")
            .assertExists()
            .assertTextContains(getString(R.string.select_option_below))
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
