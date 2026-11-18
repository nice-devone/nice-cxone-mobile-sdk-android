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

package com.nice.cxonechat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/**
 * This fraction of the shorter edge of the window the content is drawn in, as [Dp].
 *
 * Example: `0.1f.ofWindowShorterEdge()` is 10% of the shorter edge of the window.
 *
 * The window rather than the display, so that content in a window smaller than the display — one side of
 * a split screen, a freeform window, or a foldable folded shut — is sized for the space it actually has.
 * The shorter edge rather than the width, so that a square measured from it fits whichever way round the
 * window is.
 */
@Composable
internal fun Float.ofWindowShorterEdge(): Dp {
    val windowSize = LocalWindowInfo.current.containerDpSize
    return minOf(windowSize.width, windowSize.height) * this
}
