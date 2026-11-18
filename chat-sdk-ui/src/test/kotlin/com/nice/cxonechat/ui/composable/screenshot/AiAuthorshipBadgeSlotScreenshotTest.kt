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

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.AiAuthorshipBadgeSlot
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the two presentations of [AiAuthorshipBadgeSlot], the AI authorship disclosure carried by
 * both variants of the conversation top app bar.
 *
 * The `singleRow` flag is what these goldens pin. It decides whether the badge reserves space below
 * itself to clear the two-row bar's bottom edge, and the wide-screen bar has no height to spare, so a
 * regression in that padding is a layout defect rather than a cosmetic one. The hidden state is not
 * captured here: it renders nothing at all, so an empty golden would pin nothing, and its absence is
 * already asserted by `ChatThreadTopBarAccessibilityTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AiAuthorshipBadgeSlotScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The two-row bar in portrait, where the badge pads itself away from the bar's bottom edge. */
    @Test
    fun `ai badge two row`() {
        captureBadge(singleRow = false)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `ai badge two row dark`() {
        captureBadge(singleRow = false, darkTheme = true)
    }

    /** The single-row bar used on wide windows, which drops the bottom padding. */
    @Test
    @Config(qualifiers = "w829dp-h393dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `ai badge single row`() {
        captureBadge(singleRow = true)
    }

    @Test
    @Config(qualifiers = "w829dp-h393dp-normal-notlong-notround-night-xhdpi-finger")
    fun `ai badge single row dark`() {
        captureBadge(singleRow = true, darkTheme = true)
    }

    private fun captureBadge(singleRow: Boolean, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                Surface {
                    AiAuthorshipBadgeSlot(hasAiMessages = true, singleRow = singleRow)
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }
}
