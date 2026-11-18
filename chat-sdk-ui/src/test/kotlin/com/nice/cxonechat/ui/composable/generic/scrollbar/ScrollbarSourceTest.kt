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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers both scroll sources: the exact one, which reports its own numbers, and the estimated one,
 * which has to infer them from whichever items a lazy list currently has laid out.
 *
 * The estimate is exercised through [estimateLazyMetrics] rather than through a `LazyListState`,
 * because a `LazyListState` constructed off-device reports an empty layout until something composes
 * it — which would make every assertion here vacuous.
 */
class ScrollbarSourceTest {

    private val tolerance = 0.001f

    private fun uniformItems(indices: IntRange, itemSize: Int, spacing: Int = 0, startOffset: Int = 0) =
        indices.map { index ->
            VisibleItem(
                index = index,
                offset = startOffset + (index - indices.first) * (itemSize + spacing),
                size = itemSize,
            )
        }

    /** A single cell of a vertical grid, sized and offset along the scroll axis only. */
    private fun gridCell(row: Int, column: Int, offset: Int, size: Int) = VisibleCell(
        row = row,
        column = column,
        offset = IntOffset(x = 0, y = offset),
        size = IntSize(width = CELL_WIDTH, height = size),
    )

    /** Full rows of a vertical grid, in the ascending-index order a grid reports them. */
    private fun gridCells(rows: IntRange, columns: Int, cellHeight: Int) =
        rows.flatMap { row ->
            (0 until columns).map { column ->
                gridCell(row = row, column = column, offset = row * cellHeight, size = cellHeight)
            }
        }

    @Test
    fun `a plain scroll state reports its own offset`() {
        val source = ScrollStateSource(ScrollState(initial = 250))

        assertEquals(250f, source.offset, tolerance)
    }

    @Test
    fun `uniform items give hand computed content and offset`() {
        val metrics = estimateLazyMetrics(
            visibleItems = uniformItems(0..9, itemSize = 100),
            totalCount = 50,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        // 100px average over 50 items is 5000px of content in a 1000px viewport.
        assertEquals(0f, metrics.offset, tolerance)
        assertEquals(4000f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `a partly scrolled window reports the distance already travelled`() {
        val metrics = estimateLazyMetrics(
            visibleItems = uniformItems(5..15, itemSize = 100, startOffset = -50),
            totalCount = 50,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        // Five whole items above the viewport, plus the 50px of the sixth already scrolled past.
        assertEquals(550f, metrics.offset, tolerance)
        assertEquals(4000f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `item spacing and content padding are counted in the content length`() {
        val metrics = estimateLazyMetrics(
            visibleItems = uniformItems(0..4, itemSize = 100, spacing = 10),
            totalCount = 20,
            itemSpacing = 10,
            contentPadding = 70,
            viewportLength = 500,
        )

        // 20 * 100 + 19 * 10 + 30 + 40 = 2260, less the 500px viewport.
        assertEquals(0f, metrics.offset, tolerance)
        assertEquals(1760f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `an empty list yields zeroes without dividing by the visible item count`() {
        val metrics = estimateLazyMetrics(
            visibleItems = emptyList(),
            totalCount = 0,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        assertEquals(0f, metrics.offset, tolerance)
        assertEquals(0f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `a list with items but nothing laid out yet yields zeroes`() {
        val metrics = estimateLazyMetrics(
            visibleItems = emptyList(),
            totalCount = 40,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        assertEquals(0f, metrics.offset, tolerance)
        assertEquals(0f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `the maximum offset is recomputed when the item count changes`() {
        val window = uniformItems(0..9, itemSize = 100)
        fun maxOffsetFor(totalCount: Int) = estimateLazyMetrics(
            visibleItems = window,
            totalCount = totalCount,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        ).maxOffset

        assertEquals(4000f, maxOffsetFor(totalCount = 50), tolerance)
        assertEquals(9000f, maxOffsetFor(totalCount = 100), tolerance)
    }

    @Test
    fun `a pinned first item is skipped in favour of the second`() {
        // A sticky header stays at offset zero while the item it covers has scrolled far past it.
        val metrics = estimateLazyMetrics(
            visibleItems = listOf(VisibleItem(0, 0, 60), VisibleItem(7, 0, 100), VisibleItem(8, 100, 100)),
            totalCount = 50,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        // Measured from item 7, not from the pinned header at index zero.
        assertEquals(700f, metrics.offset, tolerance)
        assertEquals(4000f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `a first item overlapping the second is treated as pinned`() {
        val metrics = estimateLazyMetrics(
            visibleItems = listOf(VisibleItem(3, 0, 100), VisibleItem(4, 50, 100)),
            totalCount = 50,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 1000,
        )

        assertEquals(350f, metrics.offset, tolerance)
    }

    @Test
    fun `a single visible item is used as its own reference`() {
        val metrics = estimateLazyMetrics(
            visibleItems = listOf(VisibleItem(4, -20, 100)),
            totalCount = 10,
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 100,
        )

        assertEquals(420f, metrics.offset, tolerance)
        assertEquals(900f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `two lazy sources over the same list stay equal, so the node is not rebuilt`() {
        val state = LazyListState()

        // Equality has to ignore the cached estimate; if it did not, every recomposition would look
        // like a new source and tear the modifier node down.
        assertEquals(LazyListSource(state, Vertical), LazyListSource(state, Vertical))
        assertEquals(LazyListSource(state, Vertical).hashCode(), LazyListSource(state, Vertical).hashCode())
        assertNotEquals(LazyListSource(state, Vertical), LazyListSource(state, Horizontal))
        assertNotEquals(LazyListSource(state, Vertical), LazyListSource(LazyListState(), Vertical))
    }

    @Test
    fun `grid cells sharing a row collapse to a single line`() {
        val lines = gridLines(gridCells(rows = 0..1, columns = 4, cellHeight = 100), Vertical)

        assertEquals(listOf(VisibleItem(0, 0, 100), VisibleItem(1, 100, 100)), lines)
    }

    @Test
    fun `a line is as tall as its tallest cell`() {
        // A grid sizes a row to its tallest member, so measuring the first cell would under-report it.
        val cells = listOf(
            gridCell(row = 0, column = 0, offset = 0, size = 100),
            gridCell(row = 0, column = 1, offset = 0, size = 140),
        )

        assertEquals(listOf(VisibleItem(0, 0, 140)), gridLines(cells, Vertical))
    }

    @Test
    fun `cells animating out report no line and are dropped`() {
        // An exiting cell keeps a stale offset, which would be read as the line's position.
        val cells = gridCells(rows = 0..0, columns = 2, cellHeight = 100) +
            gridCell(row = -1, column = -1, offset = -5000, size = 100)

        assertEquals(listOf(VisibleItem(0, 0, 100)), gridLines(cells, Vertical))
    }

    @Test
    fun `a horizontal grid reads its lines from columns rather than rows`() {
        // The same cells, transposed: laid out in one row of two columns, each column is its own line.
        val cells = listOf(
            VisibleCell(row = 0, column = 0, offset = IntOffset(0, 0), size = IntSize(80, 100)),
            VisibleCell(row = 0, column = 1, offset = IntOffset(80, 0), size = IntSize(80, 100)),
        )

        assertEquals(listOf(VisibleItem(0, 0, 80), VisibleItem(1, 80, 80)), gridLines(cells, Horizontal))
        // Read as vertical, both cells would sit on row zero and collapse into one line at the wrong size.
        assertEquals(listOf(VisibleItem(0, 0, 100)), gridLines(cells, Vertical))
    }

    @Test
    fun `the line count rounds a ragged last line up`() {
        assertEquals(2, gridLineCount(totalItemsCount = 8, maxSpan = 4))
        assertEquals(3, gridLineCount(totalItemsCount = 9, maxSpan = 4))
        assertEquals(4, gridLineCount(totalItemsCount = 7, maxSpan = 2))
        assertEquals(0, gridLineCount(totalItemsCount = 0, maxSpan = 4))
    }

    @Test
    fun `the line count is zero before the grid has measured, rather than dividing by no span`() {
        assertEquals(0, gridLineCount(totalItemsCount = 40, maxSpan = 0))
    }

    @Test
    fun `a grid reports the content length of its lines, not of its items`() {
        // Twelve cells over four columns is three lines of 100px, not twelve.
        val metrics = estimateLazyMetrics(
            visibleItems = gridLines(gridCells(rows = 0..2, columns = 4, cellHeight = 100), Vertical),
            totalCount = gridLineCount(totalItemsCount = 12, maxSpan = 4),
            itemSpacing = 0,
            contentPadding = 0,
            viewportLength = 200,
        )

        // 3 lines * 100px is 300px of content in a 200px viewport. Counting items would say 1200px.
        assertEquals(0f, metrics.offset, tolerance)
        assertEquals(100f, metrics.maxOffset, tolerance)
    }

    @Test
    fun `two grid sources over the same grid stay equal, so the node is not rebuilt`() {
        val state = LazyGridState()

        assertEquals(LazyGridSource(state, Vertical), LazyGridSource(state, Vertical))
        assertEquals(LazyGridSource(state, Vertical).hashCode(), LazyGridSource(state, Vertical).hashCode())
        assertNotEquals(LazyGridSource(state, Vertical), LazyGridSource(state, Horizontal))
        assertNotEquals(LazyGridSource(state, Vertical), LazyGridSource(LazyGridState(), Vertical))
    }

    @Test
    fun `the offset increases monotonically while scrolling forward over uneven items`() {
        // Sizes alternate, so the estimate drifts; the ordering of successive offsets must not.
        fun windowAt(firstIndex: Int, firstOffset: Int): List<VisibleItem> {
            var running = firstOffset
            return (firstIndex until firstIndex + 6).map { index ->
                val size = if (index % 2 == 0) 80 else 140
                VisibleItem(index, running, size).also { running += size }
            }
        }

        val offsets = listOf(
            estimateLazyMetrics(windowAt(0, 0), 60, 0, 0, 600).offset,
            estimateLazyMetrics(windowAt(3, -40), 60, 0, 0, 600).offset,
            estimateLazyMetrics(windowAt(9, -10), 60, 0, 0, 600).offset,
            estimateLazyMetrics(windowAt(20, -70), 60, 0, 0, 600).offset,
        )

        offsets.zipWithNext { earlier, later ->
            assertTrue("expected $later to be greater than $earlier", later > earlier)
        }
    }

    private companion object {
        /** Irrelevant to a vertical grid's estimate, which reads only the main axis. */
        const val CELL_WIDTH = 80
    }
}
