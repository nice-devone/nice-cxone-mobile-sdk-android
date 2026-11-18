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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsPropertyKey

/**
 * Test tag every scrollbar publishes, so a test can find one that draws rather than composes.
 *
 * The tag is present whether the content overflows; [ScrollbarIsVisibleKey] is what reports
 * whether anything is actually drawn.
 */
internal const val SCROLLBAR_TEST_TAG = "cxone_scrollbar"

/** The bar's bounds within the scrollable's own coordinates. */
internal val ScrollbarBarBoundsKey = SemanticsPropertyKey<Rect>("ScrollbarBarBounds")

/** The indicator's bounds within the scrollable's own coordinates. */
internal val ScrollbarIndicatorBoundsKey = SemanticsPropertyKey<Rect>("ScrollbarIndicatorBounds")

/** Displacement of the indicator from the bar's start. */
internal val ScrollbarIndicatorOffsetKey = SemanticsPropertyKey<Float>("ScrollbarIndicatorOffset")

/** Scroll axis the scrollbar follows. */
internal val ScrollbarOrientationKey = SemanticsPropertyKey<Orientation>("ScrollbarOrientation")

/** Whether a scrollbar drag is in progress. */
internal val ScrollbarIsDraggingKey = SemanticsPropertyKey<Boolean>("ScrollbarIsDragging")

/** Whether the content overflows its viewport, and a scrollbar is therefore drawn. */
internal val ScrollbarIsVisibleKey = SemanticsPropertyKey<Boolean>("ScrollbarIsVisible")
