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

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Covers the inverse of the geometry — turning a gesture into content movement — and the ordering
 * guarantee that keeps two conversions from one gesture out of each other's way.
 *
 * The fixture throughout is a 1000px viewport over 2000px of content: a 1000px bar carrying a 500px
 * indicator with 500px of travel, so one indicator pixel is worth two content pixels.
 */
class ScrollbarDragTest {

    private val tolerance = 0.001f

    private fun measuredMetrics(mirrored: Boolean = false) = ScrollbarMetrics().apply {
        pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f)
        this.mirrored = mirrored
        measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)
    }

    @Test
    fun `an indicator delta converts to the content delta it represents`() {
        val metrics = measuredMetrics()

        assertEquals(100f, metrics.contentDeltaFor(indicatorDelta = 50f), tolerance)
        assertEquals(-100f, metrics.contentDeltaFor(indicatorDelta = -50f), tolerance)
    }

    @Test
    fun `a press outside the indicator centres the indicator on the press point`() {
        val metrics = measuredMetrics()

        val offset = metrics.contentOffsetForPress(pressAlongAxis = 600f)

        assertEquals(700f, offset, tolerance)
        // Re-measuring at that offset must put the indicator's centre under the press.
        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = offset, maxScrollOffset = 1000f)
        assertEquals(600f, (metrics.indicatorStart + metrics.indicatorEnd) / 2f, tolerance)
    }

    @Test
    fun `a press past either end clamps to that end`() {
        val metrics = measuredMetrics()

        assertEquals(1000f, metrics.contentOffsetForPress(pressAlongAxis = 990f), tolerance)
        assertEquals(0f, metrics.contentOffsetForPress(pressAlongAxis = 10f), tolerance)
    }

    @Test
    fun `a press on a mirrored axis is measured from the far end`() {
        val metrics = measuredMetrics(mirrored = true)

        // Mirrored, offset zero sits at the bar's end, so a press before that means content already scrolled.
        val offset = metrics.contentOffsetForPress(pressAlongAxis = 400f)

        assertEquals(700f, offset, tolerance)
        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = offset, maxScrollOffset = 1000f)
        assertEquals(400f, (metrics.indicatorStart + metrics.indicatorEnd) / 2f, tolerance)
    }

    @Test
    fun `the published offset round-trips through dragTo on a mirrored axis`() {
        // The published offset is what a caller reads and may hand straight back. It used to be the
        // pre-mirroring placement input while dragTo took the visual one, so on a reversed axis at
        // content offset zero the round trip jumped the content to the opposite end.
        val metrics = measuredMetrics(mirrored = true)

        // Offset zero draws the indicator at the bar's end, so the visual displacement is the travel.
        assertEquals(metrics.travel, metrics.visualIndicatorOffset, tolerance)
        assertEquals(0f, metrics.contentOffsetForIndicator(metrics.visualIndicatorOffset), tolerance)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 600f, maxScrollOffset = 1000f)
        assertEquals(600f, metrics.contentOffsetForIndicator(metrics.visualIndicatorOffset), tolerance)
    }

    @Test
    fun `the published offset round-trips through dragTo on a plain axis`() {
        val metrics = measuredMetrics()

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 250f, maxScrollOffset = 1000f)

        assertEquals(250f, metrics.contentOffsetForIndicator(metrics.visualIndicatorOffset), tolerance)
    }

    @Test
    fun `a bar with no travel converts every press to the content start`() {
        val metrics = ScrollbarMetrics().apply {
            pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f, minIndicatorLength = 5000f)
            measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)
        }

        assertEquals(0f, metrics.contentOffsetForPress(pressAlongAxis = 600f), tolerance)
    }

    @Test
    fun `a state with no modifier attached reports nothing and moves nothing`() = runTest {
        val state = ScrollbarState()

        assertEquals(0f, state.dragTo(indicatorOffset = 100f), tolerance)
        assertEquals(0f, state.dragBy(indicatorDelta = 50f), tolerance)
        assertEquals(Rect.Zero, state.barBounds)
        assertEquals(Rect.Zero, state.indicatorBounds)
        assertEquals(0f, state.indicatorOffset, tolerance)
        assertEquals(0f, state.contentLength, tolerance)
        assertFalse(state.isVisible)
        assertFalse(state.isDragging)
    }

    @Test
    fun `successive requests from one gesture are applied in order`() = runTest {
        val source = FakeScrollbarSource()
        val scroller = ScrollbarScroller(source)

        val gesture = scroller.begin(backgroundScope)
        scroller.push(ScrollCommand.By(10f))
        scroller.push(ScrollCommand.By(20f))
        scroller.push(ScrollCommand.To(500f))
        scroller.end()
        gesture.join()

        // The third request resolves against the 30px reached by the first two, not against zero.
        assertEquals(listOf(10f, 20f, 470f), source.applied)
        assertEquals(500f, source.offset, tolerance)
    }

    @Test
    fun `a scroll to request moves by the difference from wherever the content already is`() = runTest {
        val source = FakeScrollbarSource()
        source.offset = 200f
        val scroller = ScrollbarScroller(source)

        val gesture = scroller.begin(backgroundScope)
        scroller.push(ScrollCommand.To(350f))
        scroller.end()
        gesture.join()

        assertEquals(listOf(150f), source.applied)
        assertEquals(350f, source.offset, tolerance)
    }

    @Test
    fun `a request pushed outside a gesture is dropped rather than applied later`() = runTest {
        val source = FakeScrollbarSource()
        val scroller = ScrollbarScroller(source)

        scroller.push(ScrollCommand.By(10f))
        val gesture = scroller.begin(backgroundScope)
        scroller.end()
        gesture.join()

        assertEquals(emptyList<Float>(), source.applied)
    }
}

/** Records what a gesture asked for, and moves a plain offset within a fixed range. */
private class FakeScrollbarSource(override val maxOffset: Float = 1000f) : ScrollbarSource {

    val applied = mutableListOf<Float>()

    override var offset: Float = 0f

    override val reversed: Boolean = false

    override suspend fun withUserScroll(block: suspend ScrollScope.() -> Unit) {
        val scope = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                applied += pixels
                offset = (offset + pixels).coerceIn(0f, maxOffset)
                return pixels
            }
        }
        scope.block()
    }
}
