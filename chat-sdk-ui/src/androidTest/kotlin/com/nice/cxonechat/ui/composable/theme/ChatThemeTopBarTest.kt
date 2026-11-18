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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the wide-screen behavior of [ChatTheme.TopBar].
 *
 * The window size is overridden so both sides of the width breakpoint are covered without rotating the
 * device. [DeviceConfigurationOverride.WindowSize] populates every window property the theme reads; a
 * hand-rolled [androidx.compose.ui.platform.WindowInfo] leaves the ones it omits at their defaults, and
 * `containerDpSize` then defaults to `Dp.Unspecified`, which compares as wider than any breakpoint.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class ChatThemeTopBarTest : AbstractComponentActivityUiTest() {

    private val title = "Conversation"

    @Test
    fun topBar_appliesWideScreenInset_onExpandedWidth() {
        val expectedMinimum = setTopBarInWindowWidth(EXPANDED_WIDTH)

        val titleLeft = composeTestRule.onNodeWithText(title).getUnclippedBoundsInRoot().left

        assertTrue(
            "Expected title to start at least $expectedMinimum from the left on a wide window, but was $titleLeft",
            titleLeft >= expectedMinimum,
        )
    }

    @Test
    fun topBar_appliesNoInset_onCompactWidth() {
        val wideScreenPadding = setTopBarInWindowWidth(COMPACT_WIDTH)

        val titleLeft = composeTestRule.onNodeWithText(title).getUnclippedBoundsInRoot().left

        assertTrue(
            "Expected no wide-screen inset on a compact window, but title started at $titleLeft",
            titleLeft < wideScreenPadding,
        )
    }

    @Test
    fun topBar_isAddressableByDefaultTestTag() {
        setTopBarInWindowWidth(COMPACT_WIDTH)

        composeTestRule.onNodeWithTag("chat_top_bar").assertIsDisplayed()
    }

    /**
     * Renders [ChatTheme.TopBar] in a window of [windowWidth], keeping a height tall enough that the
     * bar is never clipped.
     *
     * @return the wide-screen horizontal padding token, read during composition so tests can assert
     * against it without needing a composable context themselves.
     */
    private fun setTopBarInWindowWidth(windowWidth: Dp): Dp {
        // Dp is a value class, so it cannot be captured with lateinit.
        var wideScreenPadding: Dp? = null
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(windowWidth, WINDOW_HEIGHT)),
            ) {
                ChatTheme {
                    wideScreenPadding = space.landscapeHorizontalPadding
                    ChatTheme.TopBar(title = title)
                }
            }
        }
        return requireNotNull(wideScreenPadding) { "Theme was never composed" }
    }

    private companion object {
        /** Comfortably above [Space.expandedWidthBreakpoint]. */
        val EXPANDED_WIDTH = 1280.dp

        /** A typical portrait phone width, below [Space.expandedWidthBreakpoint]. */
        val COMPACT_WIDTH = 411.dp

        /** Taller than [EXPANDED_WIDTH], so both windows are portrait and the breakpoint decides. */
        val WINDOW_HEIGHT = 1600.dp
    }
}
