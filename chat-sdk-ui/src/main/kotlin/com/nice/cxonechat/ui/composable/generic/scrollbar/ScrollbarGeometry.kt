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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Padding resolved to pixels for the four absolute edges.
 *
 * Resolved for the layout direction before it gets here, so nothing downstream reasons about
 * leading and trailing again.
 *
 * @property left Inset from the viewport's left edge.
 * @property top Inset from the viewport's top edge.
 * @property right Inset from the viewport's right edge.
 * @property bottom Inset from the viewport's bottom edge.
 */
internal data class ScrollbarEdges(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

/**
 * A [ScrollbarConfig] with every density-dependent value already converted to pixels.
 *
 * Converted once per density or config change rather than on every draw pass.
 *
 * @property indicatorThickness Cross-axis thickness of the indicator.
 * @property indicatorRadius Corner radius of the indicator.
 * @property minIndicatorLength Lower clamp on the indicator's length along the scroll axis.
 * @property maxIndicatorLength Upper clamp, or [Float.POSITIVE_INFINITY] when left unbounded.
 * @property barThickness Cross-axis thickness of the bar.
 * @property barRadius Corner radius of the bar.
 * @property padding Inset between the viewport edges and the bar.
 * @property indicatorPadding Inset shrinking the drawn indicator rectangle.
 */
internal data class ScrollbarPixels(
    val indicatorThickness: Float = 0f,
    val indicatorRadius: Float = 0f,
    val minIndicatorLength: Float = 0f,
    val maxIndicatorLength: Float = Float.POSITIVE_INFINITY,
    val barThickness: Float = 0f,
    val barRadius: Float = 0f,
    val padding: ScrollbarEdges = ScrollbarEdges(),
    val indicatorPadding: ScrollbarEdges = ScrollbarEdges(),
)

/**
 * Mutable holder for one frame's scrollbar geometry, plus the inverses that dragging needs.
 *
 * Deliberately mutable and deliberately not snapshot state: [measure] runs inside the draw phase,
 * where a snapshot write would invalidate every observer that read it. The caller owns one instance
 * and re-measures it in place, so a scrolling frame allocates nothing.
 *
 * Coordinates are split into the scroll axis (`start`/`end`) and the axis across it
 * (`crossStart`/`crossEnd`); [barRect] and [indicatorRect] fold them back into rectangles for
 * semantics and for a custom draw override.
 */
internal class ScrollbarMetrics {

    /** Scroll axis this geometry describes. */
    var orientation: Orientation = Vertical

    /** Whether indicator travel runs from the bar's end towards its start. */
    var mirrored: Boolean = false

    /** Whether a vertical bar sits on the leading cross edge, as it does in a right-to-left layout. */
    var barOnCrossStart: Boolean = false

    /** Resolved appearance for the current density. */
    var pixels: ScrollbarPixels = ScrollbarPixels()

    /** Whether the content overflows the viewport, and therefore whether anything is drawn at all. */
    var visible: Boolean = false
        private set

    /** Start of the bar along the scroll axis. */
    var barStart: Float = 0f
        private set

    /** End of the bar along the scroll axis. */
    var barEnd: Float = 0f
        private set

    /** Leading edge of the bar across the scroll axis. */
    var barCrossStart: Float = 0f
        private set

    /** Trailing edge of the bar across the scroll axis. */
    var barCrossEnd: Float = 0f
        private set

    /** Start of the drawn indicator along the scroll axis, after indicator padding. */
    var indicatorStart: Float = 0f
        private set

    /** End of the drawn indicator along the scroll axis, after indicator padding. */
    var indicatorEnd: Float = 0f
        private set

    /** Leading edge of the drawn indicator across the scroll axis, after indicator padding. */
    var indicatorCrossStart: Float = 0f
        private set

    /** Trailing edge of the drawn indicator across the scroll axis, after indicator padding. */
    var indicatorCrossEnd: Float = 0f
        private set

    /** Geometric length of the indicator before indicator padding shrinks the drawn rectangle. */
    var indicatorLength: Float = 0f
        private set

    /** Distance the indicator travels from one end of the bar to the other. */
    var travel: Float = 0f
        private set

    /** Indicator displacement from the bar's start, before mirroring — the placement input. */
    var indicatorOffset: Float = 0f
        private set

    /**
     * Indicator displacement as drawn, measured from the bar's start.
     *
     * This is what gets published and what the drag entry points take, so that everything a caller
     * sees agrees with `indicatorBounds` and round-trips: feeding this straight back into `dragTo`
     * must not move anything. On a mirrored axis the placement input runs the other way, which is why
     * the two differ at all.
     */
    val visualIndicatorOffset: Float
        get() = if (mirrored) travel - indicatorOffset else indicatorOffset

    /** Total scrollable content length along the axis. */
    var contentLength: Float = 0f
        private set

    /** Visible length along the axis. */
    var viewportLength: Float = 0f
        private set

    /** Visible length across the axis, which bounds how far the hit area can usefully reach. */
    var viewportCross: Float = 0f
        private set

    /** Largest offset the content can be scrolled to. */
    var maxScrollOffset: Float = 0f
        private set

    /** Length of the bar along the scroll axis. */
    val barLength: Float
        get() = barEnd - barStart

    /** Origin of the bar, for drawing. [Offset] is a value class, so reading this allocates nothing. */
    val barTopLeft: Offset
        get() = originOf(isVertical, barStart, barCrossStart)

    /** Extent of the bar, for drawing. */
    val barSize: Size
        get() = extentOf(isVertical, barEnd - barStart, barCrossEnd - barCrossStart)

    /** Origin of the drawn indicator. */
    val indicatorTopLeft: Offset
        get() = originOf(isVertical, indicatorStart, indicatorCrossStart)

    /** Extent of the drawn indicator. */
    val indicatorSize: Size
        get() = extentOf(isVertical, indicatorEnd - indicatorStart, indicatorCrossEnd - indicatorCrossStart)

    private val isVertical: Boolean
        get() = orientation == Vertical

    /**
     * Recomputes every published value from one frame's inputs.
     *
     * Nothing is published when the content fits: values from an earlier frame are cleared first, so
     * a stale bar can never be reported as visible or be hit-tested against.
     */
    fun measure(viewportWidth: Float, viewportHeight: Float, contentOffset: Float, maxScrollOffset: Float) {
        reset()

        val isVertical = orientation == Vertical
        val axisLength = if (isVertical) viewportHeight else viewportWidth
        if (maxScrollOffset <= 0f || axisLength <= 0f) return

        val crossLength = if (isVertical) viewportWidth else viewportHeight
        val padding = pixels.padding
        val leadingPad = if (isVertical) padding.top else padding.left
        val trailingPad = if (isVertical) padding.bottom else padding.right

        visible = true
        viewportLength = axisLength
        viewportCross = crossLength
        contentLength = axisLength + maxScrollOffset
        this.maxScrollOffset = maxScrollOffset

        barStart = leadingPad
        barEnd = (axisLength - trailingPad).coerceAtLeast(barStart)
        placeBarAcross(isVertical, crossLength, padding)
        placeIndicator(isVertical, axisLength, contentOffset)
    }

    /**
     * Converts a movement of the indicator into the movement of the content it represents.
     *
     * On a mirrored axis the two run opposite ways, so moving the indicator towards the bar's start
     * moves the content towards its end.
     */
    fun contentDeltaFor(indicatorDelta: Float): Float {
        if (travel <= 0f) return 0f
        val alongContent = if (mirrored) -indicatorDelta else indicatorDelta
        return alongContent * maxScrollOffset / travel
    }

    /**
     * Converts an absolute indicator displacement into the content offset it represents, clamped to
     * the scrollable range.
     */
    fun contentOffsetForIndicator(indicatorOffset: Float): Float {
        if (travel <= 0f) return 0f
        val unmirrored = if (mirrored) travel - indicatorOffset else indicatorOffset
        return (unmirrored * maxScrollOffset / travel).coerceIn(0f, maxScrollOffset)
    }

    /**
     * Converts a press along the scroll axis into the content offset that centres the indicator
     * under it.
     *
     * Measured from the bar's start rather than from the inflated hit area, so the indicator's
     * centre really does land under the finger instead of being offset by the grab margin.
     */
    fun contentOffsetForPress(pressAlongAxis: Float): Float =
        contentOffsetForIndicator(pressAlongAxis - barStart - indicatorLength / 2f)

    /** The bar's bounds in the viewport's coordinates. */
    fun barRect(): Rect = rectOf(isVertical, barStart, barEnd, barCrossStart, barCrossEnd)

    /** The drawn indicator's bounds in the viewport's coordinates. */
    fun indicatorRect(): Rect = rectOf(isVertical, indicatorStart, indicatorEnd, indicatorCrossStart, indicatorCrossEnd)

    /**
     * The bar's bounds grown by [grabMargin] on every side, for hit testing a thin bar.
     *
     * Clamped to the viewport on both axes, because no pointer event is ever delivered outside it —
     * an unclamped rectangle would report a reachable area larger than the one that exists.
     */
    fun hitRect(grabMargin: Float): Rect = if (!visible) {
        Rect.Zero
    } else {
        rectOf(
            isVertical,
            (barStart - grabMargin).coerceAtLeast(0f),
            (barEnd + grabMargin).coerceAtMost(viewportLength),
            (barCrossStart - grabMargin).coerceAtLeast(0f),
            (barCrossEnd + grabMargin).coerceAtMost(viewportCross),
        )
    }

    private fun placeBarAcross(isVertical: Boolean, crossLength: Float, padding: ScrollbarEdges) {
        // A horizontal bar always hugs the bottom edge; only a vertical one swaps sides for the layout direction.
        if (isVertical && barOnCrossStart) {
            barCrossStart = padding.left
            barCrossEnd = barCrossStart + pixels.barThickness
        } else {
            barCrossEnd = crossLength - if (isVertical) padding.right else padding.bottom
            barCrossStart = barCrossEnd - pixels.barThickness
        }
    }

    private fun placeIndicator(isVertical: Boolean, axisLength: Float, contentOffset: Float) {
        val bar = barLength
        val proportional = if (contentLength > 0f) bar * axisLength / contentLength else 0f
        indicatorLength = proportional
            .coerceAtMost(bar)
            .coerceAtLeast(pixels.minIndicatorLength)
            .coerceAtMost(pixels.maxIndicatorLength)
            .coerceAtMost(bar)
        travel = (bar - indicatorLength).coerceAtLeast(0f)
        indicatorOffset = if (travel > 0f) {
            (contentOffset * travel / maxScrollOffset).coerceIn(0f, travel)
        } else {
            0f
        }

        val drawnStart = if (mirrored) barEnd - indicatorLength - indicatorOffset else barStart + indicatorOffset
        val crossCentre = (barCrossStart + barCrossEnd) / 2f
        val crossStart = crossCentre - pixels.indicatorThickness / 2f
        val inset = pixels.indicatorPadding

        // Indicator padding insets a box the size of the indicator, not the bar, so an inset wider
        // than what it insets would otherwise invert the rectangle. An inverted rect draws nothing
        // *and* reports false from `contains` for every point, which would silently turn every press
        // on the indicator into a jump. Collapsing to empty keeps the failure honest.
        indicatorStart = drawnStart + if (isVertical) inset.top else inset.left
        indicatorEnd = (drawnStart + indicatorLength - if (isVertical) inset.bottom else inset.right)
            .coerceAtLeast(indicatorStart)
        indicatorCrossStart = crossStart + if (isVertical) inset.left else inset.top
        indicatorCrossEnd = (crossStart + pixels.indicatorThickness - if (isVertical) inset.right else inset.bottom)
            .coerceAtLeast(indicatorCrossStart)
    }

    private fun reset() {
        visible = false
        barStart = 0f
        barEnd = 0f
        barCrossStart = 0f
        barCrossEnd = 0f
        indicatorStart = 0f
        indicatorEnd = 0f
        indicatorCrossStart = 0f
        indicatorCrossEnd = 0f
        indicatorLength = 0f
        travel = 0f
        indicatorOffset = 0f
        contentLength = 0f
        viewportLength = 0f
        viewportCross = 0f
        maxScrollOffset = 0f
    }
}

private fun originOf(vertical: Boolean, start: Float, crossStart: Float): Offset =
    if (vertical) Offset(crossStart, start) else Offset(start, crossStart)

private fun extentOf(vertical: Boolean, along: Float, across: Float): Size =
    if (vertical) Size(across, along) else Size(along, across)

private fun rectOf(vertical: Boolean, start: Float, end: Float, crossStart: Float, crossEnd: Float): Rect =
    if (vertical) {
        Rect(left = crossStart, top = start, right = crossEnd, bottom = end)
    } else {
        Rect(left = start, top = crossStart, right = end, bottom = crossEnd)
    }
