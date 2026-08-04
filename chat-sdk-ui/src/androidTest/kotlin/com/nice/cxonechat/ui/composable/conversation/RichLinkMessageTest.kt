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

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.nice.cxonechat.ui.util.preview.message.Media as PreviewMedia

@RunWith(AndroidJUnit4::class)
class RichLinkMessageTest : AbstractComponentActivityUiTest() {
    @get:Rule
    val intentsRule = IntentsRule()

    @Test
    fun richLinkMessage_rendersCorrectly_andIsClickable() {
        val richLink = RichLink(UiSdkRichLink())
        val testColorPair = ColorPair(foreground = Color.Black, background = Color.White)
        composeTestRule.setContent {
            RichLinkMessage(message = richLink, textColor = testColorPair)
        }
        // Card is displayed
        composeTestRule.onNodeWithTag("rich_link_message").assertIsDisplayed()
        // Image is displayed & has correct content description
        composeTestRule.onNodeWithTag("rich_link_image", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                getString(string.content_description_rich_link_image, richLink.title)
            )
        // Title is displayed
        composeTestRule.onNodeWithText(richLink.title).assertIsDisplayed()
        // URL is displayed
        composeTestRule.onNodeWithText(richLink.url).assertIsDisplayed()
        // Link icon is displayed
        composeTestRule.onNodeWithTag("rich_link_icon", useUnmergedTree = true).assertIsDisplayed()

        // Set up intent stubbing to intercept and prevent browser launch
        intending(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(richLink.url)
            )
        ).respondWith(ActivityResult(Activity.RESULT_OK, null))

        // Card is clickable
        composeTestRule.onNodeWithTag("rich_link_message")
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick() // Also triggers accessibility checks

        // Verify Intent was created with correct action and URL
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(richLink.url)
            )
        )
    }

    @Test
    fun richLinkMessage_withBlankMediaUrl_imageHasNoContentDescription() {
        val richLink = RichLink(
            UiSdkRichLink(
                media = PreviewMedia(url = "", mimeType = "image/jpeg", fileName = "Preview Image")
            )
        )
        val testColorPair = ColorPair(foreground = Color.Black, background = Color.White)
        composeTestRule.setContent {
            RichLinkMessage(message = richLink, textColor = testColorPair)
        }
        composeTestRule.onNodeWithTag("rich_link_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rich_link_image", useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .tryPerformAccessibilityChecks()
    }
}
