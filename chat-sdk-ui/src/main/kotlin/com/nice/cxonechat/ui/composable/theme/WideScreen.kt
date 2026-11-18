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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

/**
 * Whether the window is wider than it is tall.
 *
 * Derived from the window rather than the device orientation, so it remains correct in split-screen
 * and multi-window, where the device may be landscape while this window is tall and narrow.
 */
@Composable
internal fun isLandscapeWindow(): Boolean {
    val containerSize = LocalWindowInfo.current.containerSize
    return containerSize.width > containerSize.height
}

/**
 * Whether the content is drawn in a window which should use the wide-screen layout.
 *
 * True for any landscape window, and for a portrait window at least
 * [Space.expandedWidthBreakpoint] wide, such as a tablet held upright.
 *
 * Landscape alone is enough because a window in landscape is always wider than the same device in
 * portrait, and because the width breakpoint on its own excludes devices the layout is meant for: a
 * phone in landscape sits either side of it depending on the model, and both an unfolded foldable and
 * a landscape tablet fall below it.
 *
 * Single source of truth for the wide-screen decision: the conversation scaffold, the message frame,
 * the thread list and its FAB all read from here so they can't drift out of alignment.
 */
@Composable
internal fun isWideScreen(): Boolean {
    if (isLandscapeWindow()) return true
    val containerWidthDp = LocalWindowInfo.current.containerDpSize.width
    return containerWidthDp >= space.expandedWidthBreakpoint
}

/**
 * Horizontal padding to apply to content on wide screens, or `0.dp` on compact ones.
 *
 * @see isWideScreen
 */
@Composable
internal fun wideScreenHorizontalPadding(): Dp =
    if (isWideScreen()) space.landscapeHorizontalPadding else 0.dp

/**
 * Width an audio attachment cell may grow to in [available] space: all of it, so the cell fills its bubble.
 *
 * Never narrower than its playback row, which cannot wrap and is clipped rather than shrunk, so a narrow window
 * would otherwise drop a control off the edge with nothing to show for it.
 *
 * @param available Width the message bubble gives its content, which a message already sits inside.
 * @see audioCellMaxWidthInWindow
 */
@Composable
internal fun audioCellMaxWidth(available: Dp): Dp =
    maxOf(available, audioCellPlaybackRowWidth())

/**
 * Width an audio attachment cell may grow to in a [window] this wide, with no message bubble around it to narrow the
 * space first.
 *
 * For previews. A message reaches the cell through a bubble already capped at [Space.messageBubbleMaxWidthFraction],
 * so a preview measuring the window has to take that share itself or it draws a cell wider than any message can be.
 */
@Composable
internal fun audioCellMaxWidthInWindow(window: Dp): Dp =
    audioCellMaxWidth(window * space.messageBubbleMaxWidthFraction)

/**
 * Width the cell needs to hold its playback controls: each takes a full touch target whatever glyph it paints, and
 * the row sits inside the cell's horizontal padding.
 */
@Composable
internal fun audioCellPlaybackRowWidth(): Dp {
    val layoutDirection = LocalLayoutDirection.current
    return space.clickableSize * PLAYBACK_CONTROL_COUNT +
        space.audioPlayerIconSpacing * (PLAYBACK_CONTROL_COUNT - 1) +
        space.audioMessagePadding.calculateStartPadding(layoutDirection) +
        space.audioMessagePadding.calculateEndPadding(layoutDirection)
}

/** Seek back, play/pause, seek forward. */
private const val PLAYBACK_CONTROL_COUNT = 3

/**
 * The width a modal bottom sheet may grow to.
 *
 * Material 3 caps a sheet at [BottomSheetDefaults.SheetMaxWidth] and centres it, which keeps a form
 * readable on a desktop-sized window but leaves a landscape phone showing a narrow card with unusable
 * space either side. In a landscape window the sheet spans the window instead.
 *
 * A sheet whose own design draws it as a centred card in landscape declines this through
 * [WideScreenModalBottomSheet]'s `sheetMaxWidth`, as the attachment picker, the end conversation sheet
 * and the inactivity prompt do.
 *
 * This decides the sheet's outer width only. Where the content inside it sits is each sheet's own choice,
 * since a sheet mixes full-bleed elements with inset ones - see [wideScreenHorizontalPadding], which the
 * sheets that line their content up with the conversation apply themselves.
 *
 * Gated on [isLandscapeWindow] rather than the broader [isWideScreen] used elsewhere in this file: a
 * window that is wide but still taller than it is wide - a portrait tablet, or an unfolded foldable held
 * upright - has the height to show a centred sheet as designed, so it keeps the cap.
 *
 * [Dp.Unspecified] is Material 3's documented way to ask for a sheet as wide as its window.
 *
 * @see isLandscapeWindow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun wideScreenSheetMaxWidth(): Dp =
    if (isLandscapeWindow()) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth

/**
 * Scroll behavior for the conversation's top app bar.
 *
 * A wide window draws a single-row bar, which has no collapsed state to scroll into, so it is pinned
 * there and the conversation keeps its own scroll deltas. An enter-always behavior would go on taking
 * those deltas to drive a height offset that such a bar never applies.
 *
 * @see isWideScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberTopBarScrollBehavior(): TopAppBarScrollBehavior =
    if (isWideScreen()) {
        TopAppBarDefaults.pinnedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }

/**
 * Window insets for a top app bar which inset its content on wide screens.
 *
 * Top app bars use insets rather than a padding modifier so that the bar background keeps spanning
 * the full window width while only its content is inset.
 *
 * The horizontal amount is applied symmetrically, so it is layout-direction agnostic despite
 * [WindowInsets] taking physical left/right values. Asymmetric wide-screen padding would need to
 * resolve [androidx.compose.ui.platform.LocalLayoutDirection] first.
 *
 * @param base Insets to add the wide-screen padding to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun wideScreenTopAppBarInsets(base: WindowInsets = TopAppBarDefaults.windowInsets): WindowInsets {
    if (!isWideScreen()) return base
    val horizontalInsetPx = with(LocalDensity.current) { space.landscapeHorizontalPadding.roundToPx() }
    return base.add(WindowInsets(left = horizontalInsetPx, right = horizontalInsetPx))
}
