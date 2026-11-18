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

package com.nice.cxonechat.ui.composable.generic.scrollbar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Exercises the scrollbar through the semantics surface it publishes, which is the only way to
 * assert against a component that draws itself instead of composing anything.
 *
 * The fixture is a 200x400dp column at xhdpi, so the viewport is 400x800px and the bar — 8dp thick,
 * against the right edge — occupies x 384..400. Eight 100dp rows make 1600px of content, giving
 * 800px of scroll, an indicator half the bar's 800px length, and 400px of travel: one indicator
 * pixel is worth two content pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScrollbarSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scrollState = ScrollState(initial = 0)
    private val scrollbarState = ScrollbarState()
    private val onBar = 392f

    @Test
    fun `an overflowing content reports a visible scrollbar inside its viewport`() {
        showColumn(rowCount = 8)

        val config = scrollbarSemantics()
        assertTrue(config[ScrollbarIsVisibleKey])

        val viewport = Rect(0f, 0f, 400f, 800f)
        val bar = config[ScrollbarBarBoundsKey]
        val indicator = config[ScrollbarIndicatorBoundsKey]
        assertFalse(bar.isEmpty)
        assertFalse(indicator.isEmpty)
        assertEquals(bar, bar.intersect(viewport))
        assertEquals(indicator, indicator.intersect(viewport))
    }

    @Test
    fun `content that fits reports no scrollbar and publishes no geometry`() {
        showColumn(rowCount = 2)

        val config = scrollbarSemantics()

        assertFalse(config[ScrollbarIsVisibleKey])
        assertTrue(config[ScrollbarBarBoundsKey].isEmpty)
        assertTrue(config[ScrollbarIndicatorBoundsKey].isEmpty)
        assertEquals(0f, config[ScrollbarIndicatorOffsetKey], 0.001f)
    }

    @Test
    fun `the indicator offset grows as the content scrolls`() {
        showColumn(rowCount = 8)
        assertEquals(0f, scrollbarState.indicatorOffset, 0.001f)

        drag(fromY = 10f, toY = 110f)

        // 200px of content over 800px, against 400px of travel, puts the indicator 100px along.
        assertEquals(100f, scrollbarState.indicatorOffset, 0.001f)
        // The semantics copy is refreshed at the deliberate invalidation points only, and the end of
        // a drag is one of them, so it has caught up by the time the gesture is over.
        assertEquals(100f, scrollbarSemantics()[ScrollbarIndicatorOffsetKey], 0.001f)
    }

    @Test
    fun `the drag flag is raised for the length of the gesture and cleared after it`() {
        showColumn(rowCount = 8)
        val node = composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG)

        node.performTouchInput { down(Offset(onBar, 10f)) }
        composeTestRule.waitForIdle()
        assertTrue(scrollbarState.isDragging)
        assertTrue(scrollbarSemantics()[ScrollbarIsDraggingKey])

        node.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertFalse(scrollbarState.isDragging)
        assertFalse(scrollbarSemantics()[ScrollbarIsDraggingKey])
    }

    @Test
    fun `a drag on the bar moves the content by the conversion alone`() {
        showColumn(rowCount = 8)

        // 100px down the bar, against 400px of travel over 800px of content, is 200px of content.
        // Anything else means the content also scrolled from the same gesture, which is what the
        // Initial pointer pass and the suppressed touch scrolling exist to prevent.
        drag(fromY = 10f, toY = 110f)

        assertEquals(200, scrollState.value)
    }

    @Test
    fun `a press off the indicator jumps its centre under the press point`() {
        showColumn(rowCount = 8)

        // The indicator covers 0..400, so 500 is past it: centring it there needs 600px of content.
        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).performTouchInput {
            down(Offset(onBar, 500f))
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(600, scrollState.value)
    }

    @Test
    fun `a press away from the bar leaves the scrollbar alone`() {
        showColumn(rowCount = 8)

        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).performTouchInput {
            down(Offset(20f, 400f))
            up()
        }
        composeTestRule.waitForIdle()

        assertFalse(scrollbarState.isDragging)
        assertEquals(0, scrollState.value)
    }

    @Test
    fun `a draw override replaces the default painting and receives both rectangles`() {
        val drawn = mutableListOf<Pair<Rect, Rect>>()
        val config = ScrollbarConfig(draw = { bar, indicator -> drawn += bar to indicator })

        showColumn(rowCount = 8, config = config)

        val (bar, indicator) = drawn.last()
        assertFalse(bar.isEmpty)
        assertFalse(indicator.isEmpty)
        // The same rectangles the default painting would have used.
        assertEquals(scrollbarState.barBounds, bar)
        assertEquals(scrollbarState.indicatorBounds, indicator)
    }

    @Test
    fun `a disabled scrollbar draws but does not scroll the content when its bar is dragged`() {
        showColumn(rowCount = 8, enabled = false)

        // The bar is still painted, so the geometry is published as usual.
        assertTrue(scrollbarSemantics()[ScrollbarIsVisibleKey])

        drag(fromY = 10f, toY = 110f)

        // enabled=false has to reach the scrollbar too. Gating only the scroll modifier left the
        // indicator as the one way to scroll a scrollable the caller had switched off.
        assertEquals(0, scrollState.value)
        assertFalse(scrollbarState.isDragging)
    }

    @Test
    fun `a drag retargeted part-way through follows the geometry it is redrawn with`() {
        // reverseScrolling is part of the source's identity, so flipping it mid-drag retargets the
        // node. The gesture must finish against the scroller it opened: replacing that queue used to
        // drop every remaining move while the handler kept consuming the events — a drag that looks
        // alive and does nothing.
        //
        // It finishes against the *new* geometry, though, not the geometry it started in. The bar is
        // redrawn mirrored under the finger, so following it is what the user sees; reverseScrolling is
        // fixed per call site anyway, which is why the node keeps no per-gesture copy to follow instead.
        // A quarter scrolled, not half. At half, mirroring places the indicator on [200, 600] either way,
        // so the redraw the comment below describes would not actually happen. Here it moves from
        // [100, 500] to [300, 700], which is what a finger would see.
        val retargetScrollState = ScrollState(initial = 200)
        var reversed by mutableStateOf(false)
        composeTestRule.setContent {
            ChatTheme {
                CompositionLocalProvider(LocalScrollbarConfig provides ScrollbarConfig()) {
                    Column(
                        modifier = Modifier
                            .size(width = 200.dp, height = 400.dp)
                            .verticalScrollWithScrollbar(
                                retargetScrollState,
                                scrollbarState,
                                reverseScrolling = reversed,
                            )
                    ) {
                        repeat(8) { Box(modifier = Modifier.fillMaxWidth().height(100.dp)) }
                    }
                }
            }
        }
        renderFrame()
        val node = composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG)

        // 400 is on the indicator both before and after the flip, so the press queues no jump either way
        // and the move is the only thing the assertion can be reading.
        node.performTouchInput { down(Offset(onBar, 400f)) }
        composeTestRule.waitForIdle()

        reversed = true
        // A drawn frame, not just an idle one: mirroring is recomputed in the draw phase, so without
        // this the move would still be converted against pre-retarget geometry and this test would
        // assert nothing about the path its name claims.
        renderFrame()

        node.performTouchInput { moveTo(Offset(onBar, 450f)) }
        node.performTouchInput { up() }
        renderFrame()

        // 50px down the mirrored bar is 100 content pixels back up: 200 - 100. Unmirrored would give 300,
        // and a move dropped by the retarget would leave 200. None of the three is clamped.
        assertEquals("the move after the retarget was mishandled", 100, retargetScrollState.value)
    }

    /**
     * The invariant the grab margin exists to protect, and the only one the geometry tests cannot reach:
     * a claim that takes far too much of the trailing edge is *correct arithmetic over the wrong input*,
     * so every assertion about the rectangle still passes while the content underneath quietly stops
     * responding.
     *
     * Both taps are dispatched at the same height through the same node, 12px apart, so the x coordinate
     * is the only thing that decides which of the two answers.
     */
    @Test
    fun `a tap on content beside the bar reaches the content and leaves the scroll alone`() {
        var taps = 0
        showColumn(rowModifier = Modifier.clickable { taps++ })

        // The claim is the 8dp bar plus a 4dp margin either side, clamped to the viewport on the trailing
        // edge: x 376..400. 368 is content, and the margin this component started with would have swallowed it.
        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).performTouchInput {
            click(Offset(368f, 500f))
        }
        renderFrame()

        assertEquals("the tap never reached the content", 1, taps)
        // The bug did not merely eat the tap — it read it as a press on the bar and jumped the content.
        assertEquals("the content jumped", 0, scrollState.value)
    }

    @Test
    fun `a tap inside the claimed strip goes to the scrollbar and not the content beneath it`() {
        var taps = 0
        showColumn(rowModifier = Modifier.clickable { taps++ })

        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).performTouchInput {
            click(Offset(380f, 500f))
        }
        renderFrame()

        assertEquals("the scrollbar let the tap through to the content", 0, taps)
        // Off the indicator, which sits at [0, 400] while the content is at the top, so the press jumps —
        // centring the 400px indicator on y 500 puts its start at 300, which is 600 content pixels. Short
        // of the 800 maximum on purpose: an expectation sitting on the clamp would pass for any overshoot.
        assertEquals(600, scrollState.value)
    }

    private fun showColumn(
        rowCount: Int = 8,
        config: ScrollbarConfig? = null,
        enabled: Boolean = true,
        state: ScrollState = scrollState,
        rowModifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            ChatTheme {
                // Provided inside the theme on purpose: ChatTheme supplies its own default, so an
                // override placed around it would simply be replaced.
                CompositionLocalProvider(LocalScrollbarConfig provides (config ?: ScrollbarConfig())) {
                    Column(
                        modifier = Modifier
                            .size(width = 200.dp, height = 400.dp)
                            .verticalScrollWithScrollbar(state, scrollbarState, enabled = enabled)
                    ) {
                        repeat(rowCount) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).then(rowModifier))
                        }
                    }
                }
            }
        }
        renderFrame()
    }

    private fun drag(fromY: Float, toY: Float) {
        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).performTouchInput {
            down(Offset(onBar, fromY))
            moveTo(Offset(onBar, toY))
            up()
        }
        renderFrame()
    }

    /**
     * Settles composition and then forces an actual draw.
     *
     * The scrollbar computes its geometry in the draw phase, so until a frame is drawn nothing is
     * published and every assertion here would read zeroes. The host view is drawn straight into a
     * throwaway bitmap rather than going through an image-capture helper, which under Robolectric
     * waits for a redraw signal that never arrives.
     */
    private fun renderFrame() {
        composeTestRule.waitForIdle()
        val view = composeTestRule.onRoot().fetchSemanticsNode().root as View
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        bitmap.recycle()
    }

    private fun scrollbarSemantics(): SemanticsConfiguration =
        composeTestRule.onNodeWithTag(SCROLLBAR_TEST_TAG).fetchSemanticsNode().config
}
