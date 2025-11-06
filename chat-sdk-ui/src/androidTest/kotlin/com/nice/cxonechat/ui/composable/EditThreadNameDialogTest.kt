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

package com.nice.cxonechat.ui.composable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test

class EditThreadNameDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editThreadNameDialog_displaysAndInteractsCorrectly() {
        var confirmed = false
        var cancelled = false

        composeTestRule.setContent {
            ChatTheme {
                EditThreadNameDialog(
                    threadName = "InitialName",
                    onCancel = { cancelled = true },
                    onAccept = { confirmed = true }
                )
            }
        }

        // Dialog and text field are displayed
        composeTestRule.onNodeWithTag("edit_thread_name_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_thread_name_text_field").assertIsDisplayed()

        // Confirm button is disabled initially
        composeTestRule.onNodeWithTag("confirm_button").assertIsNotEnabled()

        // Change text field value
        composeTestRule.onNodeWithTag("edit_thread_name_text_field")
            .performTextInput("NewName")

        // Confirm button is enabled after text change
        composeTestRule.onNodeWithTag("confirm_button").assertIsEnabled()

        composeTestRule.onNodeWithTag("confirm_button").performClick()
        assert(confirmed)

        // Click cancel button
        composeTestRule.onNodeWithTag("cancel_button").performClick()
        assert(cancelled)
    }
}
