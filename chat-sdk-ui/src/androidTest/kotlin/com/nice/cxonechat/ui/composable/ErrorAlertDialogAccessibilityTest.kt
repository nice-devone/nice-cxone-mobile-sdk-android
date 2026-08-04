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

package com.nice.cxonechat.ui.composable

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility and functionality tests for [ChatTheme.ErrorAlertDialog].
 *
 * Verifies that the error dialog displays correctly, is accessible, and that its
 * actions behave as expected, adhering to WCAG 2.2 guidelines.
 */
@RunWith(AndroidJUnit4::class)
class ErrorAlertDialogAccessibilityTest : AbstractComponentActivityUiTest() {

    private val testTitle = "Error Title"
    private val testBody = "This is the error body."
    private val testButtonText = "Close"

    /**
     * Test that the dialog displays with the correct title, body, and button text.
     * It also performs an accessibility check on the entire dialog.
     */
    @Test
    fun errorAlertDialog_displaysCorrectly_andIsAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.ErrorAlertDialog(
                    title = testTitle,
                    body = testBody,
                    buttonText = testButtonText,
                ) {}
            }
        }

        // Verify the dialog itself is displayed and accessible
        composeTestRule.onNodeWithTag("chat_error_dialog")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify title is displayed
        composeTestRule.onNodeWithText(testTitle)
            .assertIsDisplayed()

        // Verify body is displayed
        composeTestRule.onNodeWithText(testBody)
            .assertIsDisplayed()

        // Verify close button is displayed, has a click action, and is accessible
        composeTestRule.onNodeWithTag("chat_error_close_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .tryPerformAccessibilityChecks()

        // Verify button text
        composeTestRule.onNodeWithText(testButtonText)
            .assertIsDisplayed()
    }

    /**
     * Test that the onConfirmClick callback is invoked when the confirm button is clicked.
     */
    @Test
    fun errorAlertDialog_confirmClick_invokesCallback() {
        var confirmed = false
        val onConfirm = { confirmed = true }

        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.ErrorAlertDialog(
                    title = testTitle,
                    body = testBody,
                    buttonText = testButtonText,
                    onConfirmClick = onConfirm
                )
            }
        }

        // Find and click the confirm button
        composeTestRule.onNodeWithTag("chat_error_close_button")
            .performClick()

        // Assert that the callback was invoked
        assertTrue("onConfirmClick should have been called", confirmed)
    }

    /**
     * Test that the onDismissRequest is properly handled, which should be the same
     * as the confirmation click action. In this dialog, dismiss on back press or outside
     * click is disabled, so the only way to dismiss is via the confirm button.
     */
    @Test
    fun errorAlertDialog_dismissRequest_invokesCallbackViaConfirmButton() {
        var dismissed = false
        val onDismiss = { dismissed = true }

        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.ErrorAlertDialog(
                    title = testTitle,
                    body = testBody,
                    buttonText = testButtonText,
                    onConfirmClick = onDismiss // onDismissRequest is wired to this
                )
            }
        }

        // Since onDismissRequest is set to onConfirmClick, clicking the button tests both.
        composeTestRule.onNodeWithTag("chat_error_close_button")
            .performClick()

        assertTrue(
            "onConfirmClick (used for onDismissRequest) should have been called",
            dismissed
        )
    }
}
