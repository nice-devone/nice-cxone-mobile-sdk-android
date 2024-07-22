/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-specific spacing requirements.
 */
@Immutable
data class Space(
    /** a large space between items. */
    val large: Dp = 16.dp,

    /** an average space between items. */
    val medium: Dp = 8.dp,

    /** minimal space between items. */
    val small: Dp = 4.dp,

    /** default padding to be used for visual separation. */
    val defaultPadding: PaddingValues = PaddingValues(vertical = medium, horizontal = large),

    /** minimum size for any clickable elements. */
    val clickableSize: Dp = 48.dp,
)

/** current app-specific spacing requirements. */
val LocalSpace = staticCompositionLocalOf {
    Space()
}
