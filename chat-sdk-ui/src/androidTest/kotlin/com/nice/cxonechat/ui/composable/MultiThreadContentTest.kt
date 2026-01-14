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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test

class MultiThreadContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatThreadView_displaysThreadNameAndLastMessage() {
        val thread = PreviewThread.nextThread()

        composeTestRule.setContent {
            ChatTheme {
                ChatThreadView(thread) {}
            }
        }

        composeTestRule.onNodeWithTag("conversation_name",true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("last_message_time",true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Thread 1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("open conversation details").assertIsDisplayed()
    }
}
