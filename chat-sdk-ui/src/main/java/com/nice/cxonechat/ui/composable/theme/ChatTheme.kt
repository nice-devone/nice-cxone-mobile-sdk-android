/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.nice.cxonechat.ui.composable.theme.ThemeColorTokens.Companion.toDarkColorScheme
import com.nice.cxonechat.ui.composable.theme.ThemeColorTokens.Companion.toLightColorScheme

@Composable
internal fun ChatTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val theme = if (darkTheme) {
        ChatThemeDetails.darkTokens
    } else {
        ChatThemeDetails.lightTokens
    }
    val colors = if (darkTheme) {
        theme.toDarkColorScheme()
    } else {
        theme.toLightColorScheme()
    }
    val chatColors = ChatColors(theme)

    CompositionLocalProvider(
        LocalChatColors provides chatColors,
        LocalChatShapes provides ChatShapes(),
        LocalSpace provides Space(),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

internal object ChatTheme {
    /**
     * Retrieves the current [DefaultColors] at the call site's position in the hierarchy.
     */
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     */
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     */
    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    /**
     * Retrieves the [ChatColors] at the call site's position in the hierarchy.
     */
    val chatColors: ChatColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChatColors.current

    /**
     * Retrieves the [ChatTypography] at the call site.
     */
    val chatTypography: ChatTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalChatTypography.current

    /**
     * Retrieves the [ChatColors] at the call site's position in the hierarchy.
     */
    val space: Space
        @Composable
        @ReadOnlyComposable
        get() = LocalSpace.current

    /**
     * Retrieves the [ChatShapes] at the call site's position in the hierarchy.
     */
    val chatShapes: ChatShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalChatShapes.current
}
