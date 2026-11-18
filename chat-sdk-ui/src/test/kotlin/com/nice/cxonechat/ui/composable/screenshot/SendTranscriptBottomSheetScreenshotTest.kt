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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.SendTranscriptBottomSheet
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

// Wide enough to count as wide-screen (>= 840dp) while taller than wide, so the sheet keeps its 640dp cap.
private const val PORTRAIT_EXPANDED_QUALIFIERS = "w1280dp-h1600dp-normal-notlong-notround-notnight-xhdpi-finger"

/**
 * Goldens for the whole send transcript sheet in a compact portrait window, a short landscape window and a
 * portrait window wide enough to count as wide-screen.
 *
 * Robolectric dispatches no system-bar insets, so the status-bar and side-navigation-bar handling is not
 * covered here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SendTranscriptBottomSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `send transcript portrait`() {
        captureSheet()
    }

    @Test
    fun `send transcript portrait dark`() {
        captureSheet(darkTheme = true)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `send transcript landscape`() {
        captureSheet()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `send transcript landscape dark`() {
        captureSheet(darkTheme = true)
    }

    @Test
    @Config(qualifiers = PORTRAIT_EXPANDED_QUALIFIERS)
    fun `send transcript portrait expanded keeps the sheet width capped`() {
        captureSheet()
    }

    private fun captureSheet(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the scrim either side of it to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    SendTranscriptBottomSheet(
                        onDismiss = {},
                        onSubmit = {},
                        showFullScreenLoading = false,
                    )
                }
            }
        }
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(SHEET_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val SHEET_TAG = "send_transcript_bottom_sheet"
    }
}
