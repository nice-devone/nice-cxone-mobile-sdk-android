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

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [ListPickerMessage] bottom sheet functionality and status display.
 *
 * Tests verify:
 * - Bottom sheet displays title, subtitle, and action buttons correctly
 * - Cancel and Submit button callbacks are triggered
 * - Message status displays correct text and icons for SELECTABLE and SELECTED states
 * - Status component accepts Triple<ImageVector, String, Color> parameter
 */
class ListPickerBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun listPickerBottomSheet_displaysTitleSubtitleAndActions() {
        val listPicker = ListPicker(UiSdkListPicker()) {}

        composeTestRule.setContent {
            ChatTheme {
                ListPickerBottomSheet(
                    message = listPicker,
                    onDismiss = {},
                    onDone = {}
                )
            }
        }

        // The title is now the shared ChatTheme.ModalBottomSheet's own non-scrolling title, which carries
        // no test tag, so it is asserted by text instead.
        composeTestRule.onNodeWithText(listPicker.title, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = "list_picker_subtitle", useUnmergedTree = true).assertIsDisplayed()

        listPicker.actions.forEach { action ->
            val replyButton = action as? ReplyButton
            replyButton?.let {
                composeTestRule.onNodeWithText(it.text).assertIsDisplayed()
            }
        }
    }

    @Test
    fun listPickerBottomSheet_cancelButton_invokesOnDismiss() {
        val listPicker = ListPicker(UiSdkListPicker()) {}
        var dismissCalled = false

        composeTestRule.setContent {
            ChatTheme {
                ListPickerBottomSheet(
                    message = listPicker,
                    onDismiss = { dismissCalled = true },
                    onDone = {}
                )
            }
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel)).performClick()
        // Cancel goes through the shared sheet's hideSheet(), which only invokes onDismiss once the
        // hide animation's coroutine completes; performClick()'s own sync isn't guaranteed to cover that.
        composeTestRule.waitForIdle()
        assertTrue(dismissCalled)
    }

    @Test
    fun listPickerBottomSheet_selectOptionAndSubmit_invokesOnDone() {
        val listPicker = ListPicker(UiSdkListPicker()) {}
        var doneCalled = false

        composeTestRule.setContent {
            ChatTheme {
                ListPickerBottomSheet(
                    message = listPicker,
                    onDismiss = {},
                    onDone = { doneCalled = true }
                )
            }
        }

        val firstReplyButton = listPicker.actions.first() as ReplyButton
        composeTestRule.onNodeWithText(firstReplyButton.text).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.submit)).performClick()
        // Submit goes through the shared sheet's hideSheet() too; see the Cancel test above.
        composeTestRule.waitForIdle()
        assertTrue(doneCalled)
    }

    @Test
    fun messageStatusText_isDisplayed_forSelectable() {
        // Setup: Create status text and Triple manually since .asTriple() is private
        val statusText = composeTestRule.activity.getString(R.string.list_picker_open_message)

        composeTestRule.setContent {
            ChatTheme {
                // Test ListPickerMessageStatus with SELECTABLE state (TouchApp icon)
                ListPickerMessageStatus(
                    status = Triple(Icons.Default.TouchApp, statusText, Color.Blue)
                )
            }
        }

        // Verify: Status text is displayed correctly
        composeTestRule.onNodeWithText(
            text = statusText,
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun messageStatusText_isDisplayed_forSelected() {
        // Setup: Create status text and Triple manually since .asTriple() is private
        val statusText = composeTestRule.activity.getString(R.string.option_selected)

        composeTestRule.setContent {
            ChatTheme {
                // Test ListPickerMessageStatus with SELECTED state (CheckCircle icon)
                ListPickerMessageStatus(
                    status = Triple(Icons.Default.CheckCircleOutline, statusText, Color.Blue)
                )
            }
        }

        // Verify: Status text is displayed correctly
        composeTestRule.onNodeWithText(
            text = statusText,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }
}
