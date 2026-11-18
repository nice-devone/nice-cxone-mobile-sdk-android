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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.SheetTopBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the layout of [ChatTheme.SheetTopBar], which replaces the hand-rolled headers of the
 * modally presented sheets.
 *
 * The long title case pins the ellipsizing behavior: the bar has a fixed height, so a title which
 * used to wrap is now truncated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetTopBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `sheet top bar title only`() {
        captureSheetTopBar { ChatTheme.SheetTopBar(title = TITLE) }
    }

    @Test
    fun `sheet top bar with subtitle`() {
        captureSheetTopBar { ChatTheme.SheetTopBar(title = TITLE, subtitle = SUBTITLE) }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `sheet top bar with subtitle dark`() {
        captureSheetTopBar(darkTheme = true) {
            ChatTheme.SheetTopBar(title = TITLE, subtitle = SUBTITLE)
        }
    }

    @Test
    fun `sheet top bar with trailing action`() {
        captureSheetTopBar {
            ChatTheme.SheetTopBar(
                title = TITLE,
                subtitle = SUBTITLE,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                },
            )
        }
    }

    @Test
    fun `sheet top bar long title ellipsizes`() {
        captureSheetTopBar {
            ChatTheme.SheetTopBar(title = LONG_TITLE, subtitle = LONG_SUBTITLE)
        }
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `sheet top bar landscape with subtitle`() {
        captureSheetTopBar { ChatTheme.SheetTopBar(title = TITLE, subtitle = SUBTITLE) }
    }

    private fun captureSheetTopBar(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                Surface {
                    content()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val TITLE = "Select a time"
        const val SUBTITLE = "Please select a preferred time slot."
        const val LONG_TITLE = "A sheet title which is far too long to fit on a single line of the header"
        const val LONG_SUBTITLE = "A sheet subtitle which is also far too long to fit on a single line of the header"
    }
}
