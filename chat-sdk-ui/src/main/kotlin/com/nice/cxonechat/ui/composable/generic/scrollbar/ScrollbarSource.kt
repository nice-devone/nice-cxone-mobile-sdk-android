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

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One visible row of a lazy list, reduced to what the estimate in [estimateLazyMetrics] needs.
 *
 * @property index Position of the item in the whole list.
 * @property offset Offset of the item's start from the viewport's start, which is negative once scrolled past.
 * @property size Length of the item along the scroll axis.
 */
internal data class VisibleItem(val index: Int, val offset: Int, val size: Int)

/**
 * Content offset and the largest reachable offset, both along the scroll axis.
 *
 * @property offset Distance from the content's start to the viewport's start.
 * @property maxOffset Largest offset the content can reach.
 */
internal data class LazyScrollMetrics(val offset: Float, val maxOffset: Float)

/**
 * The two numbers a scrollbar needs from a scrollable, plus the way to move it.
 *
 * Concrete implementations are enumerated here rather than left open, so an unsupported scrollable
 * cannot be silently reported as a full-length indicator that never moves — the typed modifier
 * overloads simply refuse to compile for one.
 */
internal interface ScrollbarSource {
    /** Distance from the content's start to the viewport's start. */
    val offset: Float

    /** Largest offset the content can be scrolled to. */
    val maxOffset: Float

    /**
     * Whether the content is laid out from the far end, so that offset zero sits at the visual end.
     *
     * The indicator's travel mirrors to match, which is what makes a reversed list start its
     * indicator at the bottom rather than running backwards.
     */
    val reversed: Boolean

    /**
     * Runs [block] as a user-priority scroll.
     *
     * User priority is what preempts a fling still in flight, so a press on the bar takes over
     * without a stop step or a fixed delay before it.
     */
    suspend fun withUserScroll(block: suspend ScrollScope.() -> Unit)
}

/**
 * Exact source over a plain scroll state, which reports both numbers itself.
 *
 * A plain scroll state cannot say whether the caller reversed it, so [reversed] is told to it.
 *
 * @property state The scroll state driving the content.
 * @property reversed Whether the caller scrolls the content in reverse.
 */
internal data class ScrollStateSource(
    val state: ScrollState,
    override val reversed: Boolean = false,
) : ScrollbarSource {
    override val offset: Float get() = state.value.toFloat()

    override val maxOffset: Float get() = state.maxValue.toFloat()

    override suspend fun withUserScroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(MutatePriority.UserInput, block)
    }
}

/**
 * Estimated source over a lazy list, which cannot report either number directly.
 *
 * Both are re-derived whenever the visible window changes, never cached beyond it: item count and item
 * sizes both change at runtime, and a value captured for longer goes wrong as soon as either does.
 *
 * @property state The lazy list state driving the content.
 * @property orientation Axis the list scrolls along, which selects the dimension to read from its layout info.
 */
internal class LazyListSource(
    val state: LazyListState,
    val orientation: Orientation,
) : ScrollbarSource {
    private var measuredInfo: LazyListLayoutInfo? = null
    private var measured = LazyScrollMetrics(0f, 0f)

    override val offset: Float get() = metrics().offset

    override val maxOffset: Float get() = metrics().maxOffset

    /**
     * Read from the list itself rather than taken as a parameter.
     *
     * `LazyListLayoutInfo.reverseLayout` carries the raw `reverseLayout` argument; a right-to-left
     * `LazyRow` mirrors separately, through the relative placement its items use. Both therefore
     * apply, and combining them is what makes a reversed right-to-left row come out unmirrored.
     */
    override val reversed: Boolean get() = state.layoutInfo.reverseLayout

    override suspend fun withUserScroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(MutatePriority.UserInput, block)
    }

    /**
     * Estimates once per layout pass rather than once per read.
     *
     * A draw reads both published numbers, and each estimate copies the visible window into a list, so
     * an uncached read allocated twice a frame — which is exactly what the drawing path is not allowed
     * to do. The list a lazy list hands out is replaced on every measure, so its identity is a sound
     * cache key: the estimate still follows a changing window, and a frame that did not re-measure
     * costs nothing at all.
     */
    private fun metrics(): LazyScrollMetrics {
        val info = state.layoutInfo
        if (info === measuredInfo) return measured

        val viewport = if (orientation == Vertical) info.viewportSize.height else info.viewportSize.width
        measured = estimateLazyMetrics(
            visibleItems = info.visibleItemsInfo.map { VisibleItem(it.index, it.offset, it.size) },
            totalCount = info.totalItemsCount,
            itemSpacing = info.mainAxisItemSpacing,
            // Only the total matters to the estimate, so the two ends are added up here.
            contentPadding = info.beforeContentPadding + info.afterContentPadding,
            viewportLength = viewport,
        )
        measuredInfo = info
        return measured
    }

    /**
     * Identity is the list and the axis, never the cached estimate.
     *
     * Written out rather than generated because this is not a data class: the estimate it caches is
     * scratch, and two sources over the same list must stay equal so the modifier node is not torn
     * down and rebuilt on every recomposition.
     */
    override fun equals(other: Any?): Boolean {
        val that = other as? LazyListSource ?: return false
        return state == that.state && orientation == that.orientation
    }

    override fun hashCode(): Int = 31 * state.hashCode() + orientation.hashCode()
}

/**
 * Estimated source over a lazy grid, which scrolls by line rather than by item.
 *
 * A grid lays several items out side by side, so the estimate has to be fed lines: extrapolating over the
 * item count instead would report a content length as many times too long as the grid has columns, and an
 * indicator that short barely moves. Everything else is [LazyListSource]'s behaviour.
 *
 * @property state The lazy grid state driving the content.
 * @property orientation Axis the grid scrolls along, which selects both the dimension to read and whether
 * a line is a row or a column.
 */
internal class LazyGridSource(
    val state: LazyGridState,
    val orientation: Orientation,
) : ScrollbarSource {
    private var measuredInfo: LazyGridLayoutInfo? = null
    private var measured = LazyScrollMetrics(0f, 0f)

    override val offset: Float get() = metrics().offset

    override val maxOffset: Float get() = metrics().maxOffset

    override val reversed: Boolean get() = state.layoutInfo.reverseLayout

    override suspend fun withUserScroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(MutatePriority.UserInput, block)
    }

    /** Cached per layout pass, for the reason given on [LazyListSource.metrics]. */
    private fun metrics(): LazyScrollMetrics {
        val info = state.layoutInfo
        if (info === measuredInfo) return measured

        val viewport = if (orientation == Vertical) info.viewportSize.height else info.viewportSize.width
        measured = estimateLazyMetrics(
            visibleItems = gridLines(
                cells = info.visibleItemsInfo.map {
                    VisibleCell(row = it.row, column = it.column, offset = it.offset, size = it.size)
                },
                orientation = orientation,
            ),
            totalCount = gridLineCount(totalItemsCount = info.totalItemsCount, maxSpan = info.maxSpan),
            itemSpacing = info.mainAxisItemSpacing,
            contentPadding = info.beforeContentPadding + info.afterContentPadding,
            viewportLength = viewport,
        )
        measuredInfo = info
        return measured
    }

    /** Identity is the grid and the axis, never the cached estimate — see [LazyListSource.equals]. */
    override fun equals(other: Any?): Boolean {
        val that = other as? LazyGridSource ?: return false
        return state == that.state && orientation == that.orientation
    }

    override fun hashCode(): Int = 31 * state.hashCode() + orientation.hashCode()
}

/**
 * One visible grid cell, reduced to what [gridLines] needs.
 *
 * A grid reports its cells through a sealed type that cannot be constructed outside the framework, so the
 * fields the estimate reads are copied into this one and the collapsing is tested against it.
 *
 * @property row Row the cell occupies, or `-1` while it animates out.
 * @property column Column the cell occupies, or `-1` while it animates out.
 * @property offset Offset of the cell from the viewport's start, on both axes.
 * @property size Size of the cell, on both axes.
 */
internal data class VisibleCell(
    val row: Int,
    val column: Int,
    val offset: IntOffset,
    val size: IntSize,
)

/**
 * Collapses visible cells into one entry per line, which is the unit [estimateLazyMetrics] works in.
 *
 * A line's extent is the largest of its cells, matching how a grid sizes a row to its tallest member. Cells
 * still animating out report no line and are dropped, because their offsets no longer describe the layout.
 *
 * Lines come out in the order the cells arrived, which a grid reports by ascending index; the estimate
 * relies on that ordering to tell a pinned leading line from a scrolled one.
 */
internal fun gridLines(cells: List<VisibleCell>, orientation: Orientation): List<VisibleItem> {
    val vertical = orientation == Vertical
    fun VisibleCell.line() = if (vertical) row else column
    fun VisibleCell.mainAxisOffset() = if (vertical) offset.y else offset.x
    fun VisibleCell.mainAxisSize() = if (vertical) size.height else size.width

    return cells
        .filter { it.line() >= 0 }
        .groupBy { it.line() }
        .map { (line, cellsInLine) ->
            VisibleItem(
                index = line,
                offset = cellsInLine.minOf { it.mainAxisOffset() },
                size = cellsInLine.maxOf { it.mainAxisSize() },
            )
        }
}

/**
 * How many lines the whole grid holds, rounding a ragged last line up to a full one.
 *
 * Every item is taken to occupy one span, which is as much as the count can know: an item's span is
 * reported only while it is laid out, so the spans outside the visible window are unknowable. A grid that
 * overrides `span` therefore under-reports — eight items over four columns with one full-width item is
 * three lines, not two — which shortens the estimated content and leaves the indicator reaching the end of
 * the bar with content still below.
 *
 * The span count is zero until the grid has measured once, which would otherwise divide by it, and an empty
 * grid has no line to round up.
 */
internal fun gridLineCount(totalItemsCount: Int, maxSpan: Int): Int =
    if (maxSpan <= 0 || totalItemsCount <= 0) 0 else (totalItemsCount - 1) / maxSpan + 1

/**
 * Estimates a lazy list's scroll offset and maximum offset from the items it currently has laid out.
 *
 * A lazy list knows nothing about the items outside its visible window, so an average item length is
 * derived from that window and extrapolated over the whole count. With items of differing sizes the
 * average shifts as the window moves, so the indicator drifts slightly while scrolling; that is
 * inherent to estimating from a partial window, not a defect to design out.
 *
 * The first visible item is skipped when it is pinned — a sticky header, or anything else not
 * flowing normally — because its offset does not describe the scroll position.
 */
internal fun estimateLazyMetrics(
    visibleItems: List<VisibleItem>,
    totalCount: Int,
    itemSpacing: Int,
    contentPadding: Int,
    viewportLength: Int,
): LazyScrollMetrics {
    // An empty window carries no information, and averaging over it would divide by zero.
    if (visibleItems.isEmpty() || totalCount <= 0) return LazyScrollMetrics(0f, 0f)

    val referenceIndex = referenceIndexOf(visibleItems, itemSpacing)
    val reference = visibleItems[referenceIndex]
    val last = visibleItems.last()
    val spanned = visibleItems.size - referenceIndex
    val averageLineSize =
        (last.offset + last.size - reference.offset - (spanned - 1) * itemSpacing).toFloat() / spanned

    val contentLength = averageLineSize * totalCount +
        itemSpacing.toFloat() * maxOf(0, totalCount - 1) +
        contentPadding
    val offset = reference.index * (averageLineSize + itemSpacing) - reference.offset

    return LazyScrollMetrics(
        offset = offset.coerceAtLeast(0f),
        maxOffset = (contentLength - viewportLength).coerceAtLeast(0f),
    )
}

private fun referenceIndexOf(visibleItems: List<VisibleItem>, itemSpacing: Int): Int {
    if (visibleItems.size < 2) return 0
    val first = visibleItems[0]
    val second = visibleItems[1]
    val skipsIndices = second.index - first.index > 1
    val overlaps = first.offset + first.size + itemSpacing > second.offset
    return if (skipsIndices || overlaps) 1 else 0
}
