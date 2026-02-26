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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test

class SendTranscriptBottomSheetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emailFields_showError_whenEmpty() {
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptContent(onDismiss = {}, onSubmit = {})
            }
        }
        composeTestRule.onNodeWithTag("text_email").performTextInput("")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.error_required_field)
        ).assertCountEquals(2)
    }

    @Test
    fun emailFields_showError_whenInvalidEmail() {
        composeTestRule.setContent {
            SendTranscriptContent(onDismiss = {}, onSubmit = {})
        }
        composeTestRule.onNodeWithTag("text_email").performTextInput("invalid")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("invalid")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.error_email_validation)
        ).assertCountEquals(2)
    }

    @Test
    fun emailFields_showError_whenEmailsDoNotMatch() {
        composeTestRule.setContent {
            SendTranscriptContent(onDismiss = {}, onSubmit = {})
        }
        composeTestRule.onNodeWithTag("text_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("other@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.emails_do_not_match)
        ).assertIsDisplayed()
    }

    @Test
    fun submitButton_enabled_whenEmailsMatchAndValid() {
        composeTestRule.setContent {
            SendTranscriptContent(onDismiss = {}, onSubmit = {})
        }
        composeTestRule.onNodeWithTag("text_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.submit)
        ).assertIsEnabled()
    }
}
