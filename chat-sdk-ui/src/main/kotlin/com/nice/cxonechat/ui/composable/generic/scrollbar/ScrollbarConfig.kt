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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Alpha applied to the resolved content colour when the indicator colour is left unspecified. */
internal const val SCROLLBAR_INDICATOR_ALPHA = 0.45f

/** Alpha applied to the resolved content colour when the bar colour is left unspecified. */
internal const val SCROLLBAR_BAR_ALPHA = 0.12f

/**
 * How far outside the bar, on either cross-axis side, a press still counts as grabbing it.
 *
 * The scrollbar paints over content, so whatever it claims stops being tappable along the whole
 * trailing edge. Fixed at the design's margin rather than derived from [ScrollbarConfig.padding]:
 * where the bar sits is a separate decision — one screen insets it 64.dp — and a press in that gap is
 * a press on content, not on a scrollbar that is not there.
 *
 * Density-independent on purpose: inflating by raw pixels shrinks the target on a dense screen.
 */
internal val ScrollbarGrabMargin = 4.dp

/**
 * Draws the scrollbar in place of the default two rounded rectangles.
 *
 * Both rectangles are already resolved for orientation, padding and layout direction, so an
 * implementation only decides how to paint them.
 */
internal typealias ScrollbarDraw = DrawScope.(bar: Rect, indicator: Rect) -> Unit

/**
 * Appearance and geometry of a scrollbar drawn by [Modifier.scrollbar][androidx.compose.ui.Modifier].
 *
 * Both colours default to [Color.Unspecified], which resolves to the content colour in scope at
 * [SCROLLBAR_INDICATOR_ALPHA] / [SCROLLBAR_BAR_ALPHA]. A fully transparent bar colour skips the bar
 * entirely rather than painting an invisible rectangle.
 *
 * This type is annotated [Immutable] because the instance held by [LocalScrollbarConfig] never
 * carries a [draw] override. A caller that does supply one must `remember` it: an unremembered
 * lambda makes every instance compare unequal, which re-runs the modifier node update on each
 * recomposition.
 *
 * @property indicatorThickness Cross-axis thickness of the draggable indicator.
 * @property indicatorCornerRadius Corner radius of the indicator; half its thickness reads as a pill.
 * @property indicatorColor Indicator fill, or [Color.Unspecified] to derive it from the content colour.
 * @property minIndicatorLength Lower clamp on the indicator's length, so long content keeps a grabbable indicator.
 * @property maxIndicatorLength Upper clamp on the indicator's length; [Dp.Unspecified] leaves it unbounded.
 * @property barThickness Cross-axis thickness of the track the indicator travels along.
 * @property barCornerRadius Corner radius of the bar; half its thickness reads as a pill.
 * @property barColor Bar fill, or [Color.Unspecified] to derive it from the content colour.
 * @property padding Inset between the viewport edges and the bar.
 * @property indicatorPadding Inset between the bar and the indicator drawn inside it.
 * @property draw Optional override replacing the default drawing of the two rounded rectangles.
 */
@Immutable
internal data class ScrollbarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorCornerRadius: Dp = indicatorThickness / 2,
    val indicatorColor: Color = Color.Unspecified,
    val minIndicatorLength: Dp = 24.dp,
    val maxIndicatorLength: Dp = Dp.Unspecified,
    val barThickness: Dp = indicatorThickness,
    val barCornerRadius: Dp = barThickness / 2,
    val barColor: Color = Color.Unspecified,
    val padding: PaddingValues = PaddingValues(),
    val indicatorPadding: PaddingValues = PaddingValues(),
    val draw: ScrollbarDraw? = null,
)

/**
 * Cross-axis space a caller must reserve to place a scrollbar beside its content rather than over it.
 *
 * The modifier draws as an overlay and measures nothing, so reserving the strip is the caller's job. This
 * is the design's 12.dp footprint — the bar plus [ScrollbarGrabMargin] to each side of it — so a strip
 * sized from it is exactly as wide as the area a press can grab.
 *
 * The bar only ends up centred in that strip while [padding] equals [ScrollbarGrabMargin], which is where
 * the theme leaves both. Placement is [padding]'s job and reservation is this one's, so changing either
 * alone moves the bar off centre.
 */
internal val ScrollbarConfig.reservedThickness: Dp get() = barThickness + ScrollbarGrabMargin * 2

/**
 * Scrollbar appearance for the current position in the hierarchy.
 *
 * Provided by `ChatTheme`, and used whenever a scrollbar modifier is given no explicit config.
 */
internal val LocalScrollbarConfig = staticCompositionLocalOf { ScrollbarConfig() }
