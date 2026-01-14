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

package com.nice.cxonechat.sample.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import com.nice.cxonechat.ui.composable.theme.ThemeColorTokens
import com.nice.cxonechat.ui.composable.theme.DefaultColors as UIDefaultColors

/** Default color palette used by the application. */
object Colors {

    /** default colors to use in light mode. */
    object Light : ThemeColorTokens by UIDefaultColors.lightTokens

    /** default colors to use in dark mode. */
    object Dark : ThemeColorTokens by UIDefaultColors.darkTokens
}

/**
 * Expands [contentColorFor] pairs with content color for [Color.Transparent].
 * @see [contentColorFor].
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color): Color = when (backgroundColor) {
    Color.Transparent -> AppTheme.colorScheme.primary
    else -> AppTheme.colorScheme.contentColorFor(backgroundColor)
}.takeOrElse { LocalContentColor.current }
