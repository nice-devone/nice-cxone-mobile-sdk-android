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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink
import org.junit.Rule
import org.junit.Test

class RichLinkMessageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun richLinkMessage_rendersCorrectly_andIsClickable() {
        val richLink = Message.RichLink(UiSdkRichLink())
        val testColorPair = ColorPair(foreground = Color.Black, background = Color.White)
        composeTestRule.setContent {
            RichLinkMessage(message = richLink, textColor = testColorPair)
        }
        // Card is displayed
        composeTestRule.onNodeWithTag("rich_link_message")
        // Title is displayed
        composeTestRule.onNodeWithText(richLink.title).assertIsDisplayed()
        // URL is displayed
        composeTestRule.onNodeWithText(richLink.url).assertIsDisplayed()
        // Link icon is displayed
        composeTestRule.onNodeWithTag("rich_link_icon", useUnmergedTree = true).assertIsDisplayed()
        // Card is clickable
        composeTestRule.onNodeWithTag("rich_link_message")
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
    }
}
