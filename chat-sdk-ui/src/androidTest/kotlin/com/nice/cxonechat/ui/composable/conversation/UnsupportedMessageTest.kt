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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage
import org.junit.Rule
import org.junit.Test

class UnsupportedMessageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unsupportedMessage_displaysFallbackTextAndStatus() {
        composeTestRule.setContent {
            ChatTheme {
                MessageItem(
                    message = Message.Unsupported(UiSdkUnsupportedMessage()),
                    showStatus = DisplayStatus.DISPLAY,
                    messageStatusState = MessageStatusState.DISABLED,
                    onQuickReplyOptionSelected = {},
                    onListPickerSelected = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {},
                    snackBarHostState = SnackbarHostState()
                )
            }
        }

        // Check fallback text is displayed
        composeTestRule.onNodeWithText("Message cannot be displayed").assertIsDisplayed()

        // Check status indicator is displayed
        composeTestRule.onNodeWithTag("message_status_indicator", true).assertIsDisplayed()
    }

    @Test
    fun unsupportedMessageStatus_displaysIconAndText_andHandlesClick() {
        var clicked = false

        composeTestRule.setContent {
            ChatTheme {
                UnsupportedMessageStatus(onClick = { clicked = true })
            }
        }

        // Check icon and text are displayed
        composeTestRule.onNodeWithTag("unsupported_message_status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message cannot be displayed").assertIsDisplayed()

        // Check click works
        composeTestRule.onNodeWithTag("unsupported_message_status").performClick()
        assert(clicked)
    }
}
