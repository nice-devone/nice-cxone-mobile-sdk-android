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

import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scrollbar paints over content, so whatever it claims for hit testing is content that stops being
 * tappable — along the whole trailing edge, since the claim runs the length of the scrollable.
 *
 * These assertions are about *placement*, not arithmetic. An earlier version computed a margin that
 * reached a 48.dp tap target on paper and asserted only that sum, which passed while more than a third
 * of the grown area sat outside the viewport where no pointer event is ever delivered.
 */
class ScrollbarGrabMarginTest {

    /** [ScrollbarGrabMargin] in pixels; the fixtures below are raw pixels, so the density is explicit. */
    private val grabMargin = with(Density(2f)) { ScrollbarGrabMargin.toPx() }

    private fun metricsWith(padding: Float, barThickness: Float) = ScrollbarMetrics().apply {
        pixels = ScrollbarPixels(
            indicatorThickness = barThickness,
            barThickness = barThickness,
            padding = ScrollbarEdges(left = padding, top = padding, right = padding, bottom = padding),
        )
        orientation = Vertical
        measure(viewportWidth = 400f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 1000f)
    }

    @Test
    fun `the gap between a far-inset bar and the viewport edge is not claimed`() {
        val metrics = metricsWith(padding = 128f, barThickness = 8f)

        val hit = metrics.hitRect(grabMargin = grabMargin)

        // The bar sits at 264..272, and the claim stops one margin either side of it. The 128px of content
        // between it and the edge belongs to the content: there is no scrollbar pixel on it, and the
        // design applies that inset on top of the scrollbar's own footprint rather than as part of it.
        assertEquals(272f + grabMargin, hit.right, 0.001f)
        assertEquals(264f - grabMargin, hit.left, 0.001f)
    }

    @Test
    fun `the hit area never claims space beyond either end of the axis`() {
        val metrics = metricsWith(padding = 8f, barThickness = 8f)

        val hit = metrics.hitRect(grabMargin = 200f)

        assertTrue("top ${hit.top} escaped the viewport", hit.top >= 0f)
        assertTrue("bottom ${hit.bottom} escaped the viewport", hit.bottom <= 1000f)
    }

    @Test
    fun `the claimed strip is the scrollbar's own footprint and no wider`() {
        val metrics = metricsWith(padding = 8f, barThickness = 8f)

        val hit = metrics.hitRect(grabMargin = 8f)

        // The footprint is the bar plus one margin either side — 8 + 8 + 8 — and here the outward margin
        // happens to fill the 8px inset exactly, so the claim reaches the edge without passing it.
        assertEquals(400f, hit.right, 0.001f)
        assertEquals(400f - 24f, hit.left, 0.001f)
    }

    @Test
    fun `the hit area never claims space outside the viewport`() {
        val metrics = metricsWith(padding = 8f, barThickness = 8f)

        // A margin far wider than the inset must still stop at the edge rather than report phantom area.
        val hit = metrics.hitRect(grabMargin = 200f)

        assertTrue("right edge ${hit.right} escaped the viewport", hit.right <= 400f)
        assertTrue("left edge ${hit.left} escaped the viewport", hit.left >= 0f)
    }

    @Test
    fun `content that fits is not claimed at all`() {
        val metrics = ScrollbarMetrics().apply {
            pixels = ScrollbarPixels(indicatorThickness = 8f, barThickness = 8f)
            measure(viewportWidth = 400f, viewportHeight = 1000f, contentOffset = 0f, maxScrollOffset = 0f)
        }

        assertEquals(0f, metrics.hitRect(grabMargin = 8f).width, 0f)
    }
}
