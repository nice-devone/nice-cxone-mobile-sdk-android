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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test

class SendTranscriptBottomSheetTest : AbstractComponentActivityUiTest() {

    @Test
    fun emailFields_showError_whenEmpty() {
        setTestContent()
        composeTestRule.onNodeWithTag("text_email").performTextInput("")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.error_required_field)
        ).assertCountEquals(2)
    }

    @Test
    fun emailFields_showError_whenInvalidEmail() {
        setTestContent()
        composeTestRule.onNodeWithTag("text_email").performTextInput("invalid")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("invalid")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.error_email_validation)
        ).assertCountEquals(2)
    }

    @Test
    fun emailFields_showError_whenEmailsDoNotMatch() {
        setTestContent()
        composeTestRule.onNodeWithTag("text_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("other@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.emails_do_not_match)
        ).assertIsDisplayed()
    }

    @Test
    fun submitButton_enabled_whenEmailsMatchAndValid() {
        setTestContent()
        composeTestRule.onNodeWithTag("text_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.submit)
        ).assertIsEnabled()
    }

    @Test
    fun submitButton_disabled_whenEmailsEmpty() {
        setTestContent()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.submit)
        ).assertIsNotEnabled()
    }

    @Test
    fun cancelButton_callsOnDismiss() {
        var dismissCalled = false
        setTestContent(onDismiss = { dismissCalled = true })
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.cancel)
        ).performClick()
        assert(dismissCalled) { "onDismiss should be called when cancel button is clicked" }
    }

    @Test
    fun submitButton_callsOnSubmitWithEmail() {
        var submittedEmail: String? = null
        setTestContent { submittedEmail = it }
        val email = "test@example.com"
        composeTestRule.onNodeWithTag("text_email").performTextInput(email)
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput(email)
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.submit)
        ).performClick()
        assert(submittedEmail == email) { "onSubmit should be called with correct email: $email" }
    }

    // Accessibility Tests

    @Test
    fun sendTranscriptScreen_passesAccessibilityChecks() {
        setTestContent()

        // Verify screen passes accessibility checks using the title text
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.chat_transcript)
        )
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun headerElements_displayedWithProperText() {
        setTestContent()

        // Verify title text is displayed (merged semantics means we check text presence)
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.chat_transcript)
        ).assertExists()
            .assertIsDisplayed()

        // Verify subtitle/description text is displayed
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.chat_transcript_message)
        ).assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun emailFields_hasProperAccessibility() {
        setTestContent()

        // Verify email field is accessible
        composeTestRule.onNodeWithTag("text_email")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify confirm email field is accessible
        composeTestRule.onNodeWithTag("text_confirm_email")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify email label is present
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.email)
        ).assertExists()

        // Verify confirm email label is present
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.confirm_email)
        ).assertExists()
    }

    @Test
    fun buttons_haveProperAccessibility() {
        setTestContent()

        // Verify cancel button is accessible
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.cancel)
        )
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify submit button is accessible
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.submit)
        )
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun errorMessages_displayedAndAccessible() {
        setTestContent()

        // Input mismatched emails to trigger error
        composeTestRule.onNodeWithTag("text_email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput("other@example.com")
        composeTestRule.onNodeWithTag("text_confirm_email").performClick()

        // Verify error message is displayed and accessible
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.emails_do_not_match)
        )
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun allInteractiveElements_accessible_whenEmptyState() {
        setTestContent()

        // Verify all interactive elements are present and pass accessibility
        composeTestRule.onNodeWithTag("text_email")
            .assertExists()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("text_confirm_email")
            .assertExists()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
            .assertExists()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.submit))
            .assertExists()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun allInteractiveElements_accessible_whenValidState() {
        setTestContent()

        // Fill in valid emails
        val email = "test@example.com"
        composeTestRule.onNodeWithTag("text_email").performTextInput(email)
        composeTestRule.onNodeWithTag("text_confirm_email").performTextInput(email)

        // Verify all interactive elements pass accessibility when valid
        composeTestRule.onNodeWithTag("text_email")
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("text_confirm_email")
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.submit))
            .assertIsEnabled()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun emailPlaceholders_displayed() {
        setTestContent()

        // Verify placeholders are present (they should be visible when fields are empty)
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.email_placeholder)
        ).assertExists()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.confirm_email_placeholder)
        ).assertExists()
    }

    // Helper methods
    private fun setTestContent(
        onDismiss: () -> Unit = {},
        onSubmit: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptContent(onDismiss = onDismiss, onSubmit = onSubmit)
            }
        }
    }
}
