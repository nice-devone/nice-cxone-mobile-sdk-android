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

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.viewmodel.ConversationDialog
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EndContactConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_isDisplayed() {
        composeTestRule.setContent {
            ChatTheme {
                EndContactConfirmationDialog(
                    onConfirm = {},
                    onCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("end_conversation_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()
    }

    @Test
    fun confirmButton_invokesOnConfirm() {
        var confirmed = false
        var cancelled = false

        composeTestRule.setContent {
            ChatTheme {
                EndContactConfirmationDialog(
                    onConfirm = { confirmed = true },
                    onCancel = { cancelled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("confirm_button").performClick()
        assertTrue("Confirm callback should be invoked when confirm is clicked", confirmed)
        assertFalse("Cancel callback should not be invoked when confirm is clicked", cancelled)
    }

    @Test
    fun cancelButton_invokesOnCancel() {
        var confirmed = false
        var cancelled = false

        composeTestRule.setContent {
            ChatTheme {
                EndContactConfirmationDialog(
                    onConfirm = { confirmed = true },
                    onCancel = { cancelled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("cancel_button").performClick()
        assertTrue("Cancel callback should be invoked when cancel is clicked", cancelled)
        assertFalse("Confirm callback should not be invoked when cancel is clicked", confirmed)
    }

    @Test
    fun dialog_hasAccessibleTextContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val titleText = context.getString(R.string.attention)
        val confirmText = context.getString(R.string.confirm)
        val cancelText = context.getString(R.string.cancel)
        val bodyText = context.getString(R.string.livechat_end_confirmation_text)

        composeTestRule.setContent {
            ChatTheme {
                EndContactConfirmationDialog(
                    onConfirm = {},
                    onCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(bodyText).assertIsDisplayed()
        composeTestRule.onNodeWithText(confirmText).assertIsDisplayed()
        composeTestRule.onNodeWithText(cancelText).assertIsDisplayed()
    }

    @Test
    fun dialog_isDisplayed_whenEndContactConfirmationStateEmitted() {
        val dialogState = MutableStateFlow<ConversationDialog>(ConversationDialog.None)

        composeTestRule.setContent {
            ChatTheme {
                val currentDialog = dialogState.collectAsState().value
                if (currentDialog is ConversationDialog.EndContactConfirmation) {
                    EndContactConfirmationDialog(
                        onConfirm = {},
                        onCancel = { dialogState.value = ConversationDialog.None }
                    )
                }
            }
        }

        // Dialog should not be visible initially
        composeTestRule.onNodeWithTag("end_conversation_confirmation_dialog").assertDoesNotExist()

        // Simulate onEndContact triggering the confirmation dialog
        dialogState.value = ConversationDialog.EndContactConfirmation
        composeTestRule.waitForIdle()

        // Dialog should now be visible
        composeTestRule.onNodeWithTag("end_conversation_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()

        // Cancel should dismiss
        composeTestRule.onNodeWithTag("cancel_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("end_conversation_confirmation_dialog").assertDoesNotExist()
    }
}
