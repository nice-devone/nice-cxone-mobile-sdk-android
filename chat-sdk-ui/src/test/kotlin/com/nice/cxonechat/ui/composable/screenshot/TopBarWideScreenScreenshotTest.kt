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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.TopBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies that [ChatTheme.TopBar] insets its content on wide screens and leaves it flush on compact
 * ones.
 *
 * The expanded qualifier is deliberately wider than the landscape one so the wide-screen branch is
 * exercised unambiguously, rather than relying on a landscape phone happening to clear the
 * breakpoint.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopBarWideScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `top bar portrait light`() {
        captureTopBar()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `top bar portrait dark`() {
        captureTopBar(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `top bar expanded light`() {
        captureTopBar()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-night-xhdpi-finger")
    fun `top bar expanded dark`() {
        captureTopBar(darkTheme = true)
    }

    /**
     * A phone rotated to landscape is narrower than the expanded width class, but still gets the
     * wide-screen inset because the window is landscape.
     */
    @Test
    @Config(qualifiers = "w829dp-h393dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `top bar landscape phone light`() {
        captureTopBar()
    }

    /** A tablet held upright is wide, but taller still, so it is not treated as a wide window. */
    @Test
    @Config(qualifiers = "w800dp-h1280dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `top bar tall tablet light`() {
        captureTopBar()
    }

    /**
     * The wide-screen inset is expressed with physical left and right values rather than start and
     * end, so it is only layout-direction agnostic while both sides are equal. This pins that: the
     * bar has to look mirrored, not shifted, in a right-to-left layout.
     */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `top bar expanded right to left`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ChatTheme {
                    TopBarUnderTest()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private fun captureTopBar(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                TopBarUnderTest()
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Composable
    private fun TopBarUnderTest() {
        ChatTheme.TopBar(
            title = "Conversation",
            navigationIcon = { BackButton {} },
        )
    }
}
