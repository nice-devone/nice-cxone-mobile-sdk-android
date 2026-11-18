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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.SendTranscriptContent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies that the send transcript sheet stays usable in a short landscape window, where its header
 * and both email fields cannot all fit on screen at once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SendTranscriptLandscapeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `send transcript landscape reaches the confirmation field`() {
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptContent(onDismiss = {}, onSubmit = {})
            }
        }

        // The confirmation field sits below the fold in landscape, so it is only reachable if the
        // content scrolls.
        composeTestRule.onNodeWithTag("text_confirm_email").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `send transcript landscape`() {
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptContent(onDismiss = {}, onSubmit = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }
}
