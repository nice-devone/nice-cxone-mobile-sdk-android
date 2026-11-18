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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import com.nice.cxonechat.ui.composable.generic.scrollbar.ScrollbarConfig

/**
 * The designed scrollbar appearance, built from the theme's own colour and spacing tokens.
 *
 * Three details come from the design and are worth stating, because each differs from the component's
 * standalone defaults:
 *
 * - **The indicator is [Space.scrollbarThickness] thick**, half the component's default. Its corner
 *   radius stays at half the thickness, which is what makes it read as a pill.
 * - **There is no track.** No design shows one, so the bar colour is transparent — the component skips
 *   drawing a fully transparent bar rather than painting an invisible rectangle. The bar still exists
 *   geometrically, because it is what the indicator's travel and the drag hit area are measured from.
 * - **The indicator colour is `content.tertiary`**, not a literal. The design's light and dark values
 *   are exactly this token's two modes, so binding to it reproduces both without naming either.
 *
 * The inset is uniform, so it holds the bar off all four viewport edges — the ends as well as the edge it
 * hugs, which shortens the track by [Space.scrollbarInset] at each end. Deliberate: the design frames the
 * scrollbar as a component with a footprint of its own and positions that footprint inside the content,
 * one screen keeping it 64.dp from the content edge and 24.dp from the top and bottom, so a bar running
 * edge to edge is the special case rather than the rule. Insetting only the hugged edge would not be the
 * neutral alternative it looks like either: the geometry reads `bottom` as a main-axis inset for a
 * vertical bar, so `PaddingValues(end, bottom)` would shorten the bottom end and leave the top flush.
 */
internal fun chatScrollbarConfig(tokens: ThemeColorTokens, space: Space): ScrollbarConfig = ScrollbarConfig(
    indicatorThickness = space.scrollbarThickness,
    indicatorColor = tokens.content.tertiary,
    barThickness = space.scrollbarThickness,
    barColor = Color.Transparent,
    padding = PaddingValues(space.scrollbarInset),
)
