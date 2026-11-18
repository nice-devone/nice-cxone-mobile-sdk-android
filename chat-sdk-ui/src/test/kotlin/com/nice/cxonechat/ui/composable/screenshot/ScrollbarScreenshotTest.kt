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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.generic.scrollbar.SCROLLBAR_TEST_TAG
import com.nice.cxonechat.ui.composable.generic.scrollbar.ScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.horizontalScrollWithScrollbar
import com.nice.cxonechat.ui.composable.generic.scrollbar.scrollbar
import com.nice.cxonechat.ui.composable.generic.scrollbar.verticalScrollWithScrollbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins how the scrollbar actually paints, in both orientations, both themes and both layout
 * directions, at each end of the content and in the middle of it.
 *
 * These goldens are the regression net for the two placement rules that are easiest to get wrong and
 * impossible to see in a unit test: a vertical bar and its indicator must share an edge — the right
 * one normally, the left one in a right-to-left layout — and a horizontal indicator must mirror
 * inside the bar rather than inside the viewport, so padding does not shift where it lands.
 *
 * The fixture is 800px of content in a 400px viewport at xhdpi, so the indicator is half the bar and
 * `END` clamps to the far end rather than needing an exact pixel value.
 *
 * [screenshotRoborazziOptions] is the module-wide default every screenshot test shares, and nothing here
 * needs it tightened or loosened: these goldens go through `ChatTheme`, so there is no track and the
 * indicator is opaque, leaving the pill's antialiased curve as the only sub-pixel work in the frame.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScrollbarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `vertical scrollbar at start`() {
        captureVertical(scroll = START)
    }

    @Test
    fun `vertical scrollbar at middle`() {
        captureVertical(scroll = MIDDLE)
    }

    @Test
    fun `vertical scrollbar at end`() {
        captureVertical(scroll = END)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `vertical scrollbar at start dark`() {
        captureVertical(scroll = START, darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `vertical scrollbar at middle dark`() {
        captureVertical(scroll = MIDDLE, darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `vertical scrollbar at end dark`() {
        captureVertical(scroll = END, darkTheme = true)
    }

    /**
     * The bar and the indicator must both move to the left edge, never end up on opposite sides.
     * Captured at the start, which also shows that a right-to-left layout leaves vertical travel
     * alone — only the edge changes.
     */
    @Test
    fun `vertical scrollbar at start right to left`() {
        captureVertical(scroll = START, layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun `horizontal scrollbar at start`() {
        captureHorizontal(scroll = START)
    }

    @Test
    fun `horizontal scrollbar at middle`() {
        captureHorizontal(scroll = MIDDLE)
    }

    @Test
    fun `horizontal scrollbar at end`() {
        captureHorizontal(scroll = END)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `horizontal scrollbar at start dark`() {
        captureHorizontal(scroll = START, darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `horizontal scrollbar at middle dark`() {
        captureHorizontal(scroll = MIDDLE, darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `horizontal scrollbar at end dark`() {
        captureHorizontal(scroll = END, darkTheme = true)
    }

    /**
     * Captured at the content start, not the middle: mirrored travel is only visible at an end,
     * since the midpoint looks identical either way. The indicator belongs at the bar's right.
     */
    @Test
    fun `horizontal scrollbar at start right to left`() {
        captureHorizontal(scroll = START, layoutDirection = LayoutDirection.Rtl)
    }

    /**
     * A lazy grid, whose indicator length is derived from its line count rather than its item count.
     *
     * All three positions are captured, as for the scroll-state fixtures: an item-counted estimate gets
     * the indicator's length and its travel wrong in different ways, so one position would show only one
     * of the two. Positions are given in lines rather than items, which is the unit the estimate works in.
     */
    @Test
    fun `grid scrollbar at start`() {
        captureGrid(firstVisibleLine = 0)
    }

    @Test
    fun `grid scrollbar at middle`() {
        captureGrid(firstVisibleLine = GRID_LINE_LAST / 2)
    }

    @Test
    fun `grid scrollbar at end`() {
        captureGrid(firstVisibleLine = GRID_LINE_LAST)
    }

    private fun captureVertical(
        scroll: Int,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) = capture(darkTheme, layoutDirection) {
        val scrollbarState = ScrollbarState()
        Column(
            modifier = Modifier
                .size(width = VIEWPORT_SIZE.dp, height = VIEWPORT_SIZE.dp)
                .verticalScrollWithScrollbar(ScrollState(scroll), scrollbarState)
        ) {
            repeat(ROW_COUNT) { index ->
                Box(modifier = Modifier.fillMaxWidth().height(ROW_SIZE.dp).background(shadeFor(index)))
            }
        }
    }

    private fun captureHorizontal(
        scroll: Int,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) = capture(darkTheme, layoutDirection) {
        val scrollbarState = ScrollbarState()
        Row(
            modifier = Modifier
                .size(width = VIEWPORT_SIZE.dp, height = VIEWPORT_SIZE.dp)
                .horizontalScrollWithScrollbar(ScrollState(scroll), scrollbarState)
        ) {
            repeat(ROW_COUNT) { index ->
                Box(modifier = Modifier.fillMaxHeight().width(ROW_SIZE.dp).background(shadeFor(index)))
            }
        }
    }

    private fun captureGrid(firstVisibleLine: Int) = capture(darkTheme = false, LayoutDirection.Ltr) {
        val scrollbarState = ScrollbarState()
        // One state instance, shared: a second one would leave the bar reading a grid nobody scrolls.
        val gridState = LazyGridState(firstVisibleItemIndex = firstVisibleLine * GRID_COLUMNS)
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            state = gridState,
            modifier = Modifier
                .size(width = VIEWPORT_SIZE.dp, height = VIEWPORT_SIZE.dp)
                .scrollbar(gridState, scrollbarState, Vertical),
        ) {
            items(GRID_COLUMNS * GRID_LINE_COUNT) { index ->
                Box(modifier = Modifier.fillMaxWidth().height(ROW_SIZE.dp).background(shadeFor(index)))
            }
        }
    }

    private fun capture(darkTheme: Boolean, layoutDirection: LayoutDirection, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ChatTheme(darkTheme = darkTheme) {
                    Surface(content = content)
                }
            }
        }
        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Composable
    private fun shadeFor(index: Int) = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    private companion object {
        const val ROW_COUNT = 8
        const val ROW_SIZE = 50

        /** The square every fixture is laid out in, so the grid's line count follows the viewport it scrolls. */
        const val VIEWPORT_SIZE = 200

        /** The density every fixture renders at, from the xhdpi qualifier. */
        const val DENSITY = 2

        /** Four columns of eight lines, so the grid's content is twice its viewport, as the scroll fixtures are. */
        const val GRID_COLUMNS = 4
        const val GRID_LINE_COUNT = 8

        /** Lines the viewport holds at [ROW_SIZE]dp each. */
        const val GRID_LINES_VISIBLE = VIEWPORT_SIZE / ROW_SIZE

        /** The furthest line the grid scrolls to, where the indicator reaches the end of the bar. */
        const val GRID_LINE_LAST = GRID_LINE_COUNT - GRID_LINES_VISIBLE

        /** Content start. */
        const val START = 0

        /** Halfway through the available scroll, which the scroll state counts in pixels. */
        const val MIDDLE = (ROW_COUNT * ROW_SIZE - VIEWPORT_SIZE) * DENSITY / 2

        /** Past the end, which the scroll state clamps to the real maximum once it is measured. */
        const val END = Int.MAX_VALUE
    }
}
