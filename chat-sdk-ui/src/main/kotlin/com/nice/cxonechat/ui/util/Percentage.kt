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

import android.content.res.Resources
import androidx.compose.ui.unit.Dp

/**
 * Converts a double value representing fraction of display width to Dp.
 *
 * Example: `0.1.dw` is 10% of the screen width
 */
internal inline val Double.dw: Dp
    get() = Resources.getSystem().displayMetrics.let { metrics ->
        Dp(value = (this * metrics.widthPixels / metrics.density).toFloat())
    }

/**
 * Converts a double value representing a fraction of the smaller display dimension (width or height) to Dp.
 *
 * Example: `0.1.dsm` is 10% of the smaller screen dimension (useful for responsive layouts).
 */
internal inline val Double.dsm: Dp
    get() = Resources.getSystem().displayMetrics.let { metrics ->
        Dp(value = (this * minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density).toFloat())
    }

/**
 * Converts a float value representing fraction of display width to Dp.
 *
 * Example: `0.1.dw` is 10% of the screen width
 */
internal inline val Float.dw: Dp get() = this.toDouble().dw

/**
 * Converts a float value representing a fraction of the smaller display dimension (width or height) to Dp.
 *
 * Example: `0.1.dsm` is 10% of the smaller screen dimension (useful for responsive layouts).
 */
internal inline val Float.dsm: Dp get() = this.toDouble().dsm
