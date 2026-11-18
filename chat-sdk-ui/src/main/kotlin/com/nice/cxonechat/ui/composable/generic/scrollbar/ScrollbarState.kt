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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * Creates and remembers the state shared between a scrollbar modifier and its call site.
 *
 * One instance belongs to exactly one scrollbar. Sharing an instance between two of them makes one
 * of them report the other's geometry.
 */
@Composable
internal fun rememberScrollbarState(): ScrollbarState = remember { ScrollbarState() }

/**
 * Geometry published by a scrollbar, and the entry points for driving it programmatically.
 *
 * **Inert until a modifier attaches.** Before that every value reads as zero and both drag entry
 * points do nothing and report no movement; none of them throws.
 *
 * Everything except [isDragging] is a plain read of values written during measurement and drawing,
 * deliberately outside the snapshot system: a snapshot write during the draw phase invalidates
 * every observer that read it, which risks a draw-invalidate-draw cycle. [isDragging] is the one
 * exception — it is written from the pointer handler and read from composition, where it suppresses
 * the content's own scrolling for the length of a scrollbar drag.
 */
internal class ScrollbarState {

    /** Whether a scrollbar drag is currently in progress. */
    var isDragging: Boolean by mutableStateOf(false)
        internal set

    internal var metrics: ScrollbarMetrics? = null

    internal var onDragTo: (suspend (Float) -> Float)? = null

    internal var onDragBy: (suspend (Float) -> Float)? = null

    /** Scroll axis the attached scrollbar follows. */
    val orientation: Orientation
        get() = metrics?.orientation ?: Vertical

    /** Whether the content overflows its viewport, and a scrollbar is therefore drawn. */
    val isVisible: Boolean
        get() = metrics?.visible == true

    /**
     * Displacement of the indicator from the bar's start, as drawn.
     *
     * Agrees with [indicatorBounds], and round-trips through [dragTo]: passing this value back moves
     * nothing. Both are measured as seen rather than as the placement maths runs, which differ on a
     * reversed or right-to-left axis.
     */
    val indicatorOffset: Float
        get() = metrics?.visualIndicatorOffset ?: 0f

    /** The bar's bounds within the scrollable's own coordinates. */
    val barBounds: Rect
        get() = metrics?.barRect() ?: Rect.Zero

    /** The indicator's bounds within the scrollable's own coordinates. */
    val indicatorBounds: Rect
        get() = metrics?.indicatorRect() ?: Rect.Zero

    /** Length of the bar along the scroll axis. */
    val barLength: Float
        get() = metrics?.barLength ?: 0f

    /** Length of the indicator along the scroll axis. */
    val indicatorLength: Float
        get() = metrics?.indicatorLength ?: 0f

    /** Total length of the scrollable content along the axis. */
    val contentLength: Float
        get() = metrics?.contentLength ?: 0f

    /** Visible length along the axis. */
    val viewportLength: Float
        get() = metrics?.viewportLength ?: 0f

    /**
     * Moves the indicator to an absolute offset along the bar, scrolling the content to match.
     *
     * @return the content movement requested, or zero while no modifier is attached.
     */
    suspend fun dragTo(indicatorOffset: Float): Float = onDragTo?.invoke(indicatorOffset) ?: 0f

    /**
     * Moves the indicator by a delta along the bar, scrolling the content to match.
     *
     * @return the content movement requested, or zero while no modifier is attached.
     */
    suspend fun dragBy(indicatorDelta: Float): Float = onDragBy?.invoke(indicatorDelta) ?: 0f

    /**
     * Every published value, for debugging a component that draws instead of composing.
     *
     * `attached` comes first because it answers the likeliest question: until a modifier attaches,
     * everything below it reads as zero by contract rather than by fault.
     *
     * Equality is deliberately left as identity. This is a per-scrollbar channel whose values change
     * on every frame that scrolls, and [ScrollbarElement] holds one as a data-class component — so
     * value equality would make the element compare unequal mid-scroll and re-run the node update,
     * discarding the pixel cache that exists to keep the draw phase cheap.
     */
    override fun toString(): String = "ScrollbarState(" +
        "attached=${metrics != null}, " +
        "isVisible=$isVisible, " +
        "isDragging=$isDragging, " +
        "orientation=$orientation, " +
        "indicatorOffset=$indicatorOffset, " +
        "barBounds=$barBounds, " +
        "indicatorBounds=$indicatorBounds, " +
        "barLength=$barLength, " +
        "indicatorLength=$indicatorLength, " +
        "contentLength=$contentLength, " +
        "viewportLength=$viewportLength" +
        ")"
}
