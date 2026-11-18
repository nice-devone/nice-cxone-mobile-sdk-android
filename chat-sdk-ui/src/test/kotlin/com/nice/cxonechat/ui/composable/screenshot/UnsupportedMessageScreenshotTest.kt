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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.PreviewContentInset
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItem
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.isWideScreen
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode

/**
 * Goldens for the unsupported-message cell, at rest and with its tooltip shown.
 *
 * [UiSdkUnsupportedMessage] always supplies `text`, so these pin the cell's diagnostic-text branch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(Mode.NATIVE)
class UnsupportedMessageScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `unsupported message portrait`() {
        captureUnsupportedMessage()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `unsupported message portrait - dark`() {
        captureUnsupportedMessage()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `unsupported message wide screen`() {
        captureUnsupportedMessage()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `unsupported message wide screen - dark`() {
        captureUnsupportedMessage()
    }

    private fun captureUnsupportedMessage() {
        composeTestRule.setContent {
            PreviewMessageItemBase(
                message = Message.Unsupported(UiSdkUnsupportedMessage()),
                horizontalPadding = conversationColumnInset(),
            )
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `unsupported message tooltip portrait`() {
        captureUnsupportedMessageTooltip()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `unsupported message tooltip portrait - dark`() {
        captureUnsupportedMessageTooltip()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `unsupported message tooltip wide screen`() {
        captureUnsupportedMessageTooltip()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `unsupported message tooltip wide screen - dark`() {
        captureUnsupportedMessageTooltip()
    }

    private fun captureUnsupportedMessageTooltip() {
        composeTestRule.setContent {
            // The ColumnScope overload, not the `message =` one, so the spacer can share the column.
            PreviewMessageItemBase(horizontalPadding = conversationColumnInset()) {
                // Room above the anchor, so the tooltip shows above it rather than flipping below.
                Spacer(Modifier.height(200.dp))
                PreviewMessageItem(message = Message.Unsupported(UiSdkUnsupportedMessage()))
            }
        }
        composeTestRule.onNodeWithTag("unsupported_message").performClick()
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag("unsupported_message_tooltip")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /**
     * Inset the conversation column gets for the window a golden is recorded in: production's on a wide window,
     * [PreviewContentInset] on a compact one.
     */
    @Composable
    private fun conversationColumnInset(): Dp =
        if (isWideScreen()) maxOf(wideScreenHorizontalPadding(), space.large) else PreviewContentInset
}
