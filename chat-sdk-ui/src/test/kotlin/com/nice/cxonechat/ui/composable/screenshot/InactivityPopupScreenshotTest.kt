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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.InactivityContent
import com.nice.cxonechat.ui.composable.conversation.createPreviewPopup
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

/**
 * Goldens for the inactivity popup (DE-163624): [ChatTheme.SubtleModalBottomSheet] overrides
 * [com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheet]'s `sheetMaxWidth`, so it stays a
 * centred 640dp card in a landscape window instead of spanning it, matching the Figma frames for this
 * ticket.
 *
 * Renders [InactivityContent] with a fixed countdown rather than
 * [com.nice.cxonechat.ui.composable.conversation.InactivityPopup], which derives its countdown from the
 * wall clock: that would make the captured "00:xx" text depend on how long the test took to reach the
 * capture, rather than on the layout this golden exists to pin.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InactivityPopupScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `inactivity popup portrait`() {
        capturePopup()
    }

    @Test
    @Config(qualifiers = "w412dp-h852dp-normal-notlong-notround-night-xhdpi-finger")
    fun `inactivity popup portrait dark`() {
        capturePopup(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `inactivity popup landscape`() {
        capturePopup()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `inactivity popup landscape dark`() {
        capturePopup(darkTheme = true)
    }

    private fun capturePopup(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the scrim either side of it to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    InactivityContent(
                        duration = remember { mutableStateOf(FIXED_DURATION) },
                        popup = createPreviewPopup(),
                        onClickAction = {},
                        closeChat = {},
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
        const val SHEET_TAG = "inactivity_popup_bottom_sheet"

        /** Fixed rather than derived from the wall clock, so the golden's countdown text is deterministic. */
        val FIXED_DURATION = 20.seconds
    }
}
