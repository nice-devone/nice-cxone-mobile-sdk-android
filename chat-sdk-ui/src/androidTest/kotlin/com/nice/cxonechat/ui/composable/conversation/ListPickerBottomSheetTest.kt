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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker
import org.junit.Rule
import org.junit.Test

class ListPickerBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listPickerBottomSheet_displaysTitleSubtitleAndActions_andHandlesButtonClicks() {
        val listPicker = ListPicker(UiSdkListPicker()) {}
        var dismissCalled = false
        var doneCalled = false

        composeTestRule.setContent {
            ChatTheme {
                ListPickerBottomSheetContent(
                    message = listPicker,
                    onDismiss = { dismissCalled = true },
                    onDone = { doneCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("list_picker_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("list_picker_subtitle").assertIsDisplayed()

        listPicker.actions.forEach { action ->
            val replyButton = action as? ReplyButton
            replyButton?.let {
                composeTestRule.onNodeWithText(it.text).assertIsDisplayed()
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissCalled)

        val firstReplyButton = listPicker.actions.first() as ReplyButton
        composeTestRule.onNodeWithText(firstReplyButton.text).performClick()
        composeTestRule.onNodeWithText("Done").performClick()
        assert(doneCalled)
    }

    @Test
    fun messageStatusText_isDisplayed_forSelectable() {
        composeTestRule.setContent {
            ChatTheme {
                ListPickerMessageStatus(
                    messageStatusState = MessageStatusState.SELECTABLE,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Press to open", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun messageStatusText_isDisplayed_forSelected() {
        composeTestRule.setContent {
            ChatTheme {
                ListPickerMessageStatus(
                    messageStatusState = MessageStatusState.SELECTED,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Option selected", useUnmergedTree = true).assertIsDisplayed()
    }
}
