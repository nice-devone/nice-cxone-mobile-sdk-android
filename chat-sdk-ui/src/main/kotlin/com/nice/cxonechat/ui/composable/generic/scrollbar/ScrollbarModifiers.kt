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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * Scrolls vertically and draws a scrollbar over the content.
 *
 * The scrollbar is applied before the scroll modifier so it sees pointer events first and can claim
 * a gesture starting on the bar, and the caller's [enabled] flag is combined with the drag state so
 * the content cannot scroll from the same gesture that is dragging the indicator.
 *
 * Composable because it reads the drag state, which has to be re-read on each composition for the
 * suppression above to work at all — building the chain once and hoisting it would silently freeze
 * that flag.
 *
 * @param scrollState Scroll state driving the content, also read for the scrollbar's geometry.
 * @param scrollbarState Per-scrollbar state; never share one instance between two scrollbars.
 * @param enabled Whether touch scrolling is allowed — of the content, and by dragging the indicator.
 * @param flingBehavior Fling behaviour for the content, or `null` for the platform default.
 * @param reverseScrolling Whether the content scrolls in reverse; the indicator mirrors to match.
 * @param config Appearance, defaulting to the one `ChatTheme` provides.
 */
@Composable
internal fun Modifier.verticalScrollWithScrollbar(
    scrollState: ScrollState,
    scrollbarState: ScrollbarState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    config: ScrollbarConfig = LocalScrollbarConfig.current,
): Modifier = this
    .scrollbar(scrollState, scrollbarState, Vertical, reverseScrolling, config, enabled)
    .verticalScroll(
        state = scrollState,
        enabled = enabled && !scrollbarState.isDragging,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling,
    )

/**
 * Scrolls horizontally and draws a scrollbar over the content.
 *
 * The ordering and drag-suppression contract of [verticalScrollWithScrollbar] applies here too,
 * including the requirement to build the chain in composition.
 *
 * @param scrollState Scroll state driving the content, also read for the scrollbar's geometry.
 * @param scrollbarState Per-scrollbar state; never share one instance between two scrollbars.
 * @param enabled Whether touch scrolling is allowed — of the content, and by dragging the indicator.
 * @param flingBehavior Fling behaviour for the content, or `null` for the platform default.
 * @param reverseScrolling Whether the content scrolls in reverse; the indicator mirrors to match.
 * @param config Appearance, defaulting to the one `ChatTheme` provides.
 */
@Composable
internal fun Modifier.horizontalScrollWithScrollbar(
    scrollState: ScrollState,
    scrollbarState: ScrollbarState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    config: ScrollbarConfig = LocalScrollbarConfig.current,
): Modifier = this
    .scrollbar(scrollState, scrollbarState, Horizontal, reverseScrolling, config, enabled)
    .horizontalScroll(
        state = scrollState,
        enabled = enabled && !scrollbarState.isDragging,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling,
    )

/**
 * Draws a scrollbar over a container that is scrolled by other means.
 *
 * @param scrollState Scroll state to read and to drive from the scrollbar.
 * @param scrollbarState Per-scrollbar state; never share one instance between two scrollbars.
 * @param orientation Axis the scrollbar follows.
 * @param reverseScrolling Whether the content scrolls in reverse; the indicator mirrors to match.
 * @param config Appearance, defaulting to the one `ChatTheme` provides.
 * @param enabled Whether a press may drag the indicator. A disabled scrollbar still draws.
 */
@Composable
internal fun Modifier.scrollbar(
    scrollState: ScrollState,
    scrollbarState: ScrollbarState,
    orientation: Orientation,
    reverseScrolling: Boolean = false,
    config: ScrollbarConfig = LocalScrollbarConfig.current,
    enabled: Boolean = true,
): Modifier = this then ScrollbarElement(
    source = ScrollStateSource(scrollState, reverseScrolling),
    scrollbarState = scrollbarState,
    orientation = orientation,
    config = config,
    enabled = enabled,
)

/**
 * Draws a scrollbar over a lazy list, estimating its geometry from the items currently laid out.
 *
 * There is no reverse flag here: a lazy list already reports whether it was laid out in reverse, so
 * asking the caller to repeat it would only allow the two to disagree.
 *
 * @param lazyListState Lazy list state to read and to drive from the scrollbar.
 * @param scrollbarState Per-scrollbar state; never share one instance between two scrollbars.
 * @param orientation Axis the list scrolls along.
 * @param config Appearance, defaulting to the one `ChatTheme` provides.
 * @param enabled Whether a press may drag the indicator. A disabled scrollbar still draws.
 */
@Composable
internal fun Modifier.scrollbar(
    lazyListState: LazyListState,
    scrollbarState: ScrollbarState,
    orientation: Orientation,
    config: ScrollbarConfig = LocalScrollbarConfig.current,
    enabled: Boolean = true,
): Modifier = this then ScrollbarElement(
    source = LazyListSource(lazyListState, orientation),
    scrollbarState = scrollbarState,
    orientation = orientation,
    config = config,
    enabled = enabled,
)

/**
 * Draws a scrollbar over a lazy grid, estimating its geometry from the lines currently laid out.
 *
 * As with the lazy list overload there is no reverse flag, because the grid reports its own.
 *
 * Only grids whose items each occupy a single span are supported; one that overrides `span` gets an
 * indicator that stops short of its content's end, for the reason given on [gridLineCount].
 *
 * @param lazyGridState Lazy grid state to read and to drive from the scrollbar.
 * @param scrollbarState Per-scrollbar state; never share one instance between two scrollbars.
 * @param orientation Axis the grid scrolls along.
 * @param config Appearance, defaulting to the one `ChatTheme` provides.
 * @param enabled Whether a press may drag the indicator. A disabled scrollbar still draws.
 */
@Composable
internal fun Modifier.scrollbar(
    lazyGridState: LazyGridState,
    scrollbarState: ScrollbarState,
    orientation: Orientation,
    config: ScrollbarConfig = LocalScrollbarConfig.current,
    enabled: Boolean = true,
): Modifier = this then ScrollbarElement(
    source = LazyGridSource(lazyGridState, orientation),
    scrollbarState = scrollbarState,
    orientation = orientation,
    config = config,
    enabled = enabled,
)

/**
 * Both orientations, part-scrolled so the indicator sits away from either end.
 *
 * A scrollbar draws rather than composing, so it only appears once the preview renders a frame — an
 * empty-looking preview means the content fits and there is nothing to scroll.
 */
@Composable
@PreviewLightDark
private fun ScrollbarPreview() {
    ChatTheme {
        Surface {
            // The scroll state counts pixels, so a literal would land at a different point on every
            // preview density — and flush against the end on the lowest of them.
            val startAt = with(LocalDensity.current) { PREVIEW_SCROLL.roundToPx() }
            Column(
                modifier = Modifier.padding(PREVIEW_INSET),
                verticalArrangement = Arrangement.spacedBy(PREVIEW_INSET),
            ) {
                Column(
                    modifier = Modifier
                        .size(width = PREVIEW_WIDTH, height = PREVIEW_TALL)
                        .verticalScrollWithScrollbar(
                            scrollState = rememberScrollState(initial = startAt),
                            scrollbarState = rememberScrollbarState(),
                        )
                ) {
                    repeat(PREVIEW_CELLS) { index ->
                        Box(Modifier.fillMaxWidth().height(PREVIEW_CELL).background(previewShade(index)))
                    }
                }
                Row(
                    modifier = Modifier
                        .size(width = PREVIEW_WIDTH, height = PREVIEW_SHORT)
                        .horizontalScrollWithScrollbar(
                            scrollState = rememberScrollState(initial = startAt),
                            scrollbarState = rememberScrollbarState(),
                        )
                ) {
                    repeat(PREVIEW_CELLS) { index ->
                        Box(Modifier.fillMaxHeight().width(PREVIEW_CELL).background(previewShade(index)))
                    }
                }
            }
        }
    }
}

@Composable
private fun previewShade(index: Int) = if (index % 2 == 0) {
    MaterialTheme.colorScheme.surfaceVariant
} else {
    MaterialTheme.colorScheme.surfaceContainerHighest
}

private const val PREVIEW_CELLS = 8
private val PREVIEW_SCROLL = 100.dp
private val PREVIEW_INSET = 12.dp
private val PREVIEW_WIDTH = 180.dp
private val PREVIEW_TALL = 120.dp
private val PREVIEW_SHORT = 60.dp
private val PREVIEW_CELL = 40.dp
