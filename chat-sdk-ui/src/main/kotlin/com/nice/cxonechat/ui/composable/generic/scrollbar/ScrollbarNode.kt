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
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.isSpecified

/**
 * Attaches a [ScrollbarNode] to a scrollable.
 *
 * @property source The scrollable this scrollbar reads and drives.
 * @property scrollbarState State the node fills in for the call site and for tests.
 * @property orientation Axis the scrollbar follows.
 * @property config Appearance, already resolved by the composable factory that built this.
 * @property enabled Whether a press may drag the indicator. A disabled scrollbar still draws.
 */
internal data class ScrollbarElement(
    val source: ScrollbarSource,
    val scrollbarState: ScrollbarState,
    val orientation: Orientation,
    val config: ScrollbarConfig,
    val enabled: Boolean,
) : ModifierNodeElement<ScrollbarNode>() {

    override fun create(): ScrollbarNode = ScrollbarNode(source, scrollbarState, orientation, config, enabled)

    override fun update(node: ScrollbarNode) {
        node.update(source, scrollbarState, orientation, config, enabled)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollbar"
        properties["orientation"] = orientation
    }
}

/**
 * Draws a scrollbar over a scrollable's own content, and drags it.
 *
 * There is no layout node here: the viewport is whatever the container was already measured to, read
 * from the draw scope, so attaching a scrollbar costs no composition, measure or layout work.
 *
 * The draw phase therefore does the geometry, which makes three rules load-bearing rather than
 * stylistic. Nothing snapshot-backed is written while drawing — the published geometry lives in
 * plain fields on [ScrollbarMetrics] — because a snapshot write during draw invalidates every
 * observer that read it. Density-dependent values are converted once per density, layout direction,
 * config or content-colour change rather than per frame. And no allocation or coroutine happens per
 * frame: one [ScrollbarMetrics] is re-measured in place, drawing uses inline value classes, and
 * coroutines belong to gestures.
 */
internal class ScrollbarNode(
    private var source: ScrollbarSource,
    private var scrollbarState: ScrollbarState,
    private var orientation: Orientation,
    private var config: ScrollbarConfig,
    private var enabled: Boolean,
) : DelegatingNode(), DrawModifierNode, SemanticsModifierNode, CompositionLocalConsumerModifierNode {

    private val metrics = ScrollbarMetrics()

    private var scroller = ScrollbarScroller(source)

    private var cachedConfig: ScrollbarConfig? = null
    private var cachedDensity = Float.NaN
    private var cachedDirection: LayoutDirection? = null
    private var cachedContentColor = Color.Unspecified

    private var indicatorColor = Color.Unspecified
    private var barColor = Color.Unspecified
    private var grabMargin = 0f
    private var wasVisible = false
    private var republishSemantics = false

    // The handler reads every input it needs off the node as it runs, so it survives an update
    // untouched. Resetting it here would end a drag that is still in progress.
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                // awaitEachGesture rather than a loop of our own: it drains the remaining pointers
                // before starting the next gesture, so a press the scrollbar ignored cannot be
                // mistaken for the start of a new one part-way through the same touch.
                awaitEachGesture {
                    // Initial dispatches outermost first, and the scrollbar sits outside the scroll
                    // modifier it is combined with. Claiming the gesture here is what stops one press
                    // from both dragging the indicator and scrolling the content.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // Read at gesture time, so disabling takes effect without rebuilding the handler.
                    if (enabled && metrics.hitRect(grabMargin).contains(down.position)) {
                        trackGesture(down)
                    }
                }
            }
        )
    }

    override fun onAttach() {
        install()
    }

    override fun onDetach() {
        scroller.end()
        // Cleared here rather than left to the gesture's cancellation, which resumes on the
        // dispatcher: a flag stuck true would leave the caller's content permanently unscrollable.
        scrollbarState.isDragging = false
        scrollbarState.metrics = null
        scrollbarState.onDragTo = null
        scrollbarState.onDragBy = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val resolved = config
        refreshPixels(resolved, layoutDirection, currentValueOf(LocalContentColor))

        metrics.orientation = orientation
        metrics.mirrored = source.reversed xor (orientation == Horizontal && layoutDirection == Rtl)
        metrics.barOnCrossStart = orientation == Vertical && layoutDirection == Rtl
        metrics.measure(size.width, size.height, source.offset, source.maxOffset)

        if (metrics.visible != wasVisible) {
            wasVisible = metrics.visible
            invalidateSemantics()
        }
        // A gesture invalidates semantics as it starts and ends, but the geometry it produced is only
        // measured here, one draw later. Without this second, equally deliberate invalidation the
        // published bounds would stay a gesture behind for good.
        if (republishSemantics) {
            republishSemantics = false
            invalidateSemantics()
        }
        if (!metrics.visible) return

        val override = resolved.draw
        if (override == null) {
            drawScrollbar(metrics, barColor, indicatorColor)
        } else {
            override.invoke(this, metrics.barRect(), metrics.indicatorRect())
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        testTag = SCROLLBAR_TEST_TAG
        this[ScrollbarBarBoundsKey] = metrics.barRect()
        this[ScrollbarIndicatorBoundsKey] = metrics.indicatorRect()
        this[ScrollbarIndicatorOffsetKey] = metrics.visualIndicatorOffset
        this[ScrollbarOrientationKey] = orientation
        this[ScrollbarIsDraggingKey] = scrollbarState.isDragging
        this[ScrollbarIsVisibleKey] = metrics.visible
    }

    /** Re-targets an attached node when the call site passes something new. */
    fun update(
        source: ScrollbarSource,
        scrollbarState: ScrollbarState,
        orientation: Orientation,
        config: ScrollbarConfig,
        enabled: Boolean,
    ) {
        if (this.scrollbarState !== scrollbarState) {
            // The outgoing state is no longer anyone's channel, and a gesture still running will
            // clear the flag on the instance it captured rather than on this one.
            this.scrollbarState.isDragging = false
            this.scrollbarState.metrics = null
            this.scrollbarState.onDragTo = null
            this.scrollbarState.onDragBy = null
        }
        if (this.source != source) {
            // Deliberately not ended: a gesture in flight captured this instance and will end it in
            // its own `finally`. Closing the queue here would drop every remaining move while the
            // handler kept consuming the events, leaving a drag that looks alive and does nothing.
            // A scroller no gesture is using has already been ended, so nothing leaks either way.
            scroller = ScrollbarScroller(source)
        }
        this.source = source
        this.scrollbarState = scrollbarState
        this.orientation = orientation
        this.config = config
        this.enabled = enabled
        // A new config can arrive at an unchanged density, so the conversion cache cannot rely on
        // density alone to notice it.
        cachedConfig = null
        install()
        invalidateDraw()
        invalidateSemantics()
    }

    private fun install() {
        scrollbarState.metrics = metrics
        scrollbarState.onDragTo = { indicatorOffset ->
            source.applyDelta(metrics.contentOffsetForIndicator(indicatorOffset) - source.offset)
        }
        scrollbarState.onDragBy = { indicatorDelta ->
            source.applyDelta(metrics.contentDeltaFor(indicatorDelta))
        }
    }

    /**
     * Runs one gesture against the scroller and state it began with.
     *
     * An update part-way through a drag can replace either of them. Captured here so the finger keeps
     * driving the queue it opened rather than a fresh one whose channel was never started — which
     * would drop every remaining move while still consuming the events.
     */
    private suspend fun AwaitPointerEventScope.trackGesture(down: PointerInputChange) {
        val gestureScroller = scroller
        val gestureState = scrollbarState

        down.consume()
        setDragging(gestureState, true)
        // User priority preempts a fling still in flight, so the jump needs no stop step before it.
        gestureScroller.begin(coroutineScope)
        if (!metrics.indicatorRect().contains(down.position)) {
            gestureScroller.push(
                ScrollCommand.To(metrics.contentOffsetForPress(down.position.alongAxis(orientation)))
            )
        }
        try {
            followPointer(down, gestureScroller)
        } finally {
            gestureScroller.end()
            setDragging(gestureState, false)
        }
    }

    private suspend fun AwaitPointerEventScope.followPointer(
        down: PointerInputChange,
        gestureScroller: ScrollbarScroller,
    ) {
        var previous = down.position.alongAxis(orientation)
        var held = true
        while (held) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            held = change != null && change.pressed
            val current = change?.position?.alongAxis(orientation) ?: previous
            if (held && current != previous) {
                gestureScroller.push(ScrollCommand.By(metrics.contentDeltaFor(current - previous)))
            }
            previous = current
            // Consumed on the initial pass, which is what keeps the content from scrolling too.
            change?.consume()
        }
    }

    private fun setDragging(state: ScrollbarState, dragging: Boolean) {
        state.isDragging = dragging
        republishSemantics = true
        // The gesture clears the flag from its finally, which resumes on the dispatcher and so can run
        // after the node is gone. There is nothing left to invalidate then.
        if (isAttached) invalidateSemantics()
    }

    private fun Density.refreshPixels(resolved: ScrollbarConfig, direction: LayoutDirection, contentColor: Color) {
        val unchanged = resolved == cachedConfig &&
            density == cachedDensity &&
            direction == cachedDirection &&
            contentColor == cachedContentColor
        if (unchanged) return

        cachedConfig = resolved
        cachedDensity = density
        cachedDirection = direction
        cachedContentColor = contentColor

        indicatorColor = resolved.indicatorColor.takeOrElse { contentColor.copy(alpha = SCROLLBAR_INDICATOR_ALPHA) }
        barColor = resolved.barColor.takeOrElse { contentColor.copy(alpha = SCROLLBAR_BAR_ALPHA) }
        grabMargin = ScrollbarGrabMargin.toPx()
        metrics.pixels = ScrollbarPixels(
            indicatorThickness = resolved.indicatorThickness.toPx(),
            indicatorRadius = resolved.indicatorCornerRadius.toPx(),
            minIndicatorLength = resolved.minIndicatorLength.toPx(),
            maxIndicatorLength = resolved.maxIndicatorLength.toPxOrUnbounded(this),
            barThickness = resolved.barThickness.toPx(),
            barRadius = resolved.barCornerRadius.toPx(),
            padding = resolved.padding.toEdges(this, direction),
            indicatorPadding = resolved.indicatorPadding.toEdges(this, direction),
        )
    }
}

private fun Offset.alongAxis(orientation: Orientation): Float = if (orientation == Vertical) y else x

/** Scrolls by [delta] at user priority, reporting back what was asked for. */
private suspend fun ScrollbarSource.applyDelta(delta: Float): Float {
    if (delta != 0f) withUserScroll { scrollBy(delta) }
    return delta
}

/** Paints the default scrollbar: the track, unless it is fully transparent, then the indicator. */
private fun DrawScope.drawScrollbar(metrics: ScrollbarMetrics, barColor: Color, indicatorColor: Color) {
    if (barColor.alpha > 0f) {
        drawPill(barColor, metrics.pixels.barRadius, metrics.barTopLeft, metrics.barSize)
    }
    drawPill(indicatorColor, metrics.pixels.indicatorRadius, metrics.indicatorTopLeft, metrics.indicatorSize)
}

private fun DrawScope.drawPill(color: Color, radius: Float, topLeft: Offset, size: Size) {
    if (size.width <= 0f || size.height <= 0f) return
    drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = CornerRadius(radius, radius))
}

/**
 * Converts to pixels, mapping an unspecified length to no bound at all.
 *
 * `Dp.Unspecified.value` is `NaN`, so converting it would produce a bound that silently poisons
 * every comparison it takes part in.
 */
private fun Dp.toPxOrUnbounded(density: Density): Float =
    if (isSpecified) with(density) { toPx() } else Float.POSITIVE_INFINITY

private fun PaddingValues.toEdges(density: Density, direction: LayoutDirection): ScrollbarEdges = with(density) {
    ScrollbarEdges(
        left = calculateLeftPadding(direction).toPx(),
        top = calculateTopPadding().toPx(),
        right = calculateRightPadding(direction).toPx(),
        bottom = calculateBottomPadding().toPx(),
    )
}
