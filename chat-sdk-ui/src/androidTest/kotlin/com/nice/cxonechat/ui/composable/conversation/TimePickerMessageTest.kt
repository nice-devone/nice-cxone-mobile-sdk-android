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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimePickerMessageTest : AbstractComponentActivityUiTest() {

    @Test
    fun timePickerMessage_selectable_invokesOnMessageClick_andShowsNoTooltip() {
        var onMessageClickCalled = false

        composeTestRule.setContent {
            ChatTheme {
                TimePickerMessage(
                    message = TimePicker(UiSdkTimePicker()),
                    messageStatusState = SELECTABLE,
                    onMessageClick = { onMessageClickCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("time_picker_message").performClick()

        assertTrue(onMessageClickCalled)
        composeTestRule.onNodeWithText(getString(string.rich_content_disable_message)).assertDoesNotExist()
    }

    @Test
    fun timePickerMessage_disabled_showsTooltipInsteadOfInvokingOnMessageClick() {
        var onMessageClickCalled = false

        composeTestRule.setContent {
            ChatTheme {
                TimePickerMessage(
                    message = TimePicker(UiSdkTimePicker()),
                    messageStatusState = DISABLED,
                    onMessageClick = { onMessageClickCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("time_picker_message").performClick()

        assertFalse(onMessageClickCalled)
        composeTestRule.onNodeWithText(getString(string.rich_content_disable_message)).assertIsDisplayed()
    }

    @Test
    fun timePickerMessage_selected_showsTooltipInsteadOfInvokingOnMessageClick() {
        var onMessageClickCalled = false

        composeTestRule.setContent {
            ChatTheme {
                TimePickerMessage(
                    message = TimePicker(UiSdkTimePicker()),
                    messageStatusState = SELECTED,
                    onMessageClick = { onMessageClickCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("time_picker_message").performClick()

        assertFalse(onMessageClickCalled)
        composeTestRule.onNodeWithText(getString(string.rich_content_disable_message)).assertIsDisplayed()
    }
}
