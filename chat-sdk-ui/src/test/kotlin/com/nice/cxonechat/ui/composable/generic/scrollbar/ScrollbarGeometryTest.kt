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

import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives the scrollbar geometry with fixed pixel numbers.
 *
 * Everything here is plain arithmetic on [ScrollbarMetrics], so no Compose runtime, no density and
 * no Robolectric are involved. A viewport of 1000 along the axis is used throughout so the expected
 * values stay hand-checkable.
 */
class ScrollbarGeometryTest {

    private val tolerance = 0.001f

    private fun metricsOf(
        orientation: androidx.compose.foundation.gestures.Orientation = Vertical,
        mirrored: Boolean = false,
        barOnCrossStart: Boolean = false,
        pixels: ScrollbarPixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f),
    ) = ScrollbarMetrics().apply {
        this.orientation = orientation
        this.mirrored = mirrored
        this.barOnCrossStart = barOnCrossStart
        this.pixels = pixels
    }

    @Test
    fun `content fitting the viewport draws nothing`() {
        val metrics = metricsOf()

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 0f)

        assertFalse(metrics.visible)
        assertEquals(0f, metrics.barLength, tolerance)
        assertEquals(0f, metrics.indicatorLength, tolerance)
        assertEquals(0f, metrics.indicatorOffset, tolerance)
    }

    @Test
    fun `content twice the viewport gives an indicator half the bar`() {
        val metrics = metricsOf()

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertTrue(metrics.visible)
        assertEquals(1000f, metrics.barLength, tolerance)
        assertEquals(500f, metrics.indicatorLength, tolerance)
        assertEquals(0f, metrics.indicatorOffset, tolerance)
    }

    @Test
    fun `at the maximum offset the indicator is flush with the bar end`() {
        val metrics = metricsOf()

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 1000f, maxScrollOffset = 1000f)

        assertEquals(500f, metrics.indicatorOffset, tolerance)
        assertEquals(1000f, metrics.indicatorEnd, tolerance)
    }

    @Test
    fun `very long content is floored at the minimum indicator length`() {
        val metrics = metricsOf(pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f, minIndicatorLength = 24f))

        // Proportional length would be 1000 * 1000 / 100000 = 10px, below the 24px floor.
        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 99_000f)

        assertEquals(24f, metrics.indicatorLength, tolerance)
    }

    @Test
    fun `the maximum indicator length caps the proportional value`() {
        val metrics = metricsOf(pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f, maxIndicatorLength = 100f))

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(100f, metrics.indicatorLength, tolerance)
    }

    @Test
    fun `the unbounded default maximum leaves the proportional value untouched`() {
        // Dp.Unspecified resolves to POSITIVE_INFINITY rather than being converted, which would be NaN.
        val metrics = metricsOf()

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(500f, metrics.indicatorLength, tolerance)
    }

    @Test
    fun `a minimum longer than the bar never overflows the bar`() {
        val metrics = metricsOf(pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f, minIndicatorLength = 5000f))

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(1000f, metrics.indicatorLength, tolerance)
    }

    @Test
    fun `zero content and zero viewport produce zeroes without exceptions`() {
        val metrics = metricsOf()

        metrics.measure(viewportWidth = 0f, viewportHeight = 0f, contentOffset = 0f, maxScrollOffset = 0f)

        assertFalse(metrics.visible)
        assertEquals(0f, metrics.indicatorLength, tolerance)
        assertEquals(0f, metrics.indicatorOffset, tolerance)
    }

    @Test
    fun `a bar the same length as the indicator converts a drag to zero content movement`() {
        val metrics = metricsOf(pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f, minIndicatorLength = 5000f))

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(0f, metrics.travel, tolerance)
        assertEquals(0f, metrics.contentDeltaFor(indicatorDelta = 25f), tolerance)
    }

    @Test
    fun `padding shortens the bar and insets it from the trailing edge`() {
        val pixels = ScrollbarPixels(
            indicatorThickness = 8f,
            barThickness = 8f,
            padding = ScrollbarEdges(top = 10f, bottom = 30f, right = 4f),
        )
        val metrics = metricsOf(pixels = pixels)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(960f, metrics.barLength, tolerance)
        assertEquals(10f, metrics.barStart, tolerance)
        assertEquals(970f, metrics.barEnd, tolerance)
        assertEquals(88f, metrics.barCrossStart, tolerance)
        assertEquals(96f, metrics.barCrossEnd, tolerance)
    }

    @Test
    fun `a vertical bar sits on the leading cross edge when the layout is right to left`() {
        val metrics = metricsOf(barOnCrossStart = true)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(0f, metrics.barCrossStart, tolerance)
        assertEquals(8f, metrics.barCrossEnd, tolerance)
        // The indicator must share the bar's edge, never land on the opposite one.
        assertEquals(0f, metrics.indicatorCrossStart, tolerance)
        assertEquals(8f, metrics.indicatorCrossEnd, tolerance)
    }

    @Test
    fun `a horizontal bar sits on the bottom edge regardless of layout direction`() {
        val metrics = metricsOf(orientation = Horizontal, barOnCrossStart = true)

        metrics.measure(viewportWidth = 1000f, viewportHeight = 100f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(92f, metrics.barCrossStart, tolerance)
        assertEquals(100f, metrics.barCrossEnd, tolerance)
    }

    @Test
    fun `a mirrored axis starts the indicator at the bar end and walks it back`() {
        val metrics = metricsOf(mirrored = true)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(500f, metrics.indicatorStart, tolerance)
        assertEquals(1000f, metrics.indicatorEnd, tolerance)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 1000f, maxScrollOffset = 1000f)

        assertEquals(0f, metrics.indicatorStart, tolerance)
        assertEquals(500f, metrics.indicatorEnd, tolerance)
    }

    @Test
    fun `mirrored travel is measured within the bar so padding does not shift it`() {
        val pixels = ScrollbarPixels(
            indicatorThickness = 8f,
            barThickness = 8f,
            padding = ScrollbarEdges(left = 100f, right = 100f),
        )
        val metrics = metricsOf(orientation = Horizontal, mirrored = true, pixels = pixels)

        metrics.measure(viewportWidth = 1000f, viewportHeight = 100f, contentOffset = 0f, maxScrollOffset = 800f)

        // Bar spans 100..900; at offset 0 the mirrored indicator is flush with the bar's end, not the viewport's.
        assertEquals(900f, metrics.indicatorEnd, tolerance)
    }

    @Test
    fun `the indicator is centred across a thicker bar`() {
        val pixels = ScrollbarPixels(indicatorThickness = 4f, barThickness = 10f)
        val metrics = metricsOf(pixels = pixels)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(90f, metrics.barCrossStart, tolerance)
        assertEquals(100f, metrics.barCrossEnd, tolerance)
        assertEquals(93f, metrics.indicatorCrossStart, tolerance)
        assertEquals(97f, metrics.indicatorCrossEnd, tolerance)
    }

    @Test
    fun `indicator padding shrinks the indicator rectangle`() {
        val pixels = ScrollbarPixels(
            indicatorThickness = 8f,
            barThickness = 8f,
            indicatorPadding = ScrollbarEdges(left = 1f, top = 2f, right = 1f, bottom = 2f),
        )
        val metrics = metricsOf(pixels = pixels)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertEquals(2f, metrics.indicatorStart, tolerance)
        assertEquals(498f, metrics.indicatorEnd, tolerance)
        assertEquals(93f, metrics.indicatorCrossStart, tolerance)
        assertEquals(99f, metrics.indicatorCrossEnd, tolerance)
    }

    @Test
    fun `an indicator inset wider than the indicator collapses it rather than inverting it`() {
        // 4px either side of an 8px-thick indicator leaves nothing; more than that used to invert the
        // rectangle, which draws nothing *and* makes contains() false everywhere — so every press on
        // the indicator would be classified as a press outside it and jump the scroll.
        val pixels = ScrollbarPixels(
            indicatorThickness = 8f,
            barThickness = 8f,
            indicatorPadding = ScrollbarEdges(left = 20f, top = 900f, right = 20f, bottom = 900f),
        )
        val metrics = metricsOf(pixels = pixels)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)

        assertTrue("start ${metrics.indicatorStart} past end ${metrics.indicatorEnd}", metrics.indicatorEnd >= metrics.indicatorStart)
        assertTrue(metrics.indicatorCrossEnd >= metrics.indicatorCrossStart)
        assertTrue("rect inverted: ${metrics.indicatorRect()}", !metrics.indicatorRect().let { it.right < it.left || it.bottom < it.top })
    }

    @Test
    fun `measuring content that fits clears geometry published by an earlier frame`() {
        val metrics = metricsOf()
        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 500f, maxScrollOffset = 1000f)
        assertTrue(metrics.visible)

        metrics.measure(viewportWidth = 100f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 0f)

        assertFalse(metrics.visible)
        assertEquals(0f, metrics.barEnd, tolerance)
        assertEquals(0f, metrics.indicatorEnd, tolerance)
        assertEquals(0f, metrics.indicatorCrossEnd, tolerance)
    }
}
