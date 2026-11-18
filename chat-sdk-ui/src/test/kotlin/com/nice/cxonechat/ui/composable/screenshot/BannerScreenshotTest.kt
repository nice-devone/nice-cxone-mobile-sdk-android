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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.HeaderBar
import com.nice.cxonechat.ui.composable.LeadingContentPreviewContent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.nice.cxonechat.ui.screen.HeaderBar as OfflineHeaderBar

/**
 * Golden coverage for the two `ListItem` based banners.
 *
 * Neither appeared in any existing golden: the position in queue banner is gated behind a queue position no
 * preview state supplies, and the offline banner had no screenshot test at all. Both moved to the overload
 * that takes the headline as trailing content, which also brought `verticalAlignment`, `shapes` and
 * `elevation` defaults with it. These pin what those defaults render, which the tag assertions cannot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BannerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `position in queue banner`() {
        capture { PositionInQueueBanner() }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `position in queue banner dark`() {
        capture(darkTheme = true) { PositionInQueueBanner() }
    }

    /**
     * At 411dp the bar is narrower than [space.componentMaxWidth] and never hits the cap, so this pins the
     * width-capped, centered look on a window wide enough to trigger it - the same 852dp landscape frame used
     * for the offline screen goldens (node 63133-47909).
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-long-notround-notnight-xhdpi-finger")
    fun `position in queue banner wide window`() {
        capture { PositionInQueueBanner() }
    }

    @Test
    fun `offline banner`() {
        capture { OfflineHeaderBar() }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `offline banner dark`() {
        capture(darkTheme = true) { OfflineHeaderBar() }
    }

    /**
     * Rebuilds the call site rather than calling `PositionInQueue`, which wraps the bar in a Lottie animation
     * a golden cannot pin. The stand-in matches the real leading box, so the slot alignment still gets covered.
     */
    @Composable
    private fun PositionInQueueBanner() {
        HeaderBar(
            containerColor = chatColors.token.background.surface.emphasis,
            titleText = stringResource(string.position_in_queue_next),
            messageText = stringResource(string.position_in_queue_supporting_text),
            leadingContent = { LeadingContentPreviewContent() },
        )
    }

    /**
     * The banner has to grow with the text rather than clip it, so this case is the one that tells a minimum
     * height apart from a fixed one. Two hard-wrapped supporting lines leave the least room, and `fontScale`
     * is not a Robolectric qualifier, hence the density override.
     */
    @Test
    fun `position in queue banner at large font scale`() {
        capture(fontScale = LARGE_FONT_SCALE) { PositionInQueueBanner() }
    }

    private fun capture(
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChatTheme(darkTheme = darkTheme) {
                    Surface { content() }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        /** Past the point where the supporting text stops fitting a fixed height bar. */
        const val LARGE_FONT_SCALE = 1.75f
    }
}
