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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
internal fun ChatTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val theme = if (darkTheme) {
        ChatThemeDetails.darkColors
    } else {
        ChatThemeDetails.lightColors
    }
    val colors = if (darkTheme) {
        darkColors(
            primary = theme.primary,
            onPrimary = theme.onPrimary,
            background = theme.background,
            onBackground = theme.onBackground,
            surface = theme.background,
            onSurface = theme.onBackground,
            secondary = theme.accent,
            onSecondary = theme.onAccent,
            secondaryVariant = theme.accent.rippleVariant(),
        )
    } else {
        lightColors(
            primary = theme.primary,
            onPrimary = theme.onPrimary,
            background = theme.background,
            onBackground = theme.onBackground,
            surface = theme.background,
            onSurface = theme.onBackground,
            secondary = theme.accent,
            onSecondary = theme.onAccent,
            secondaryVariant = theme.accent.rippleVariant(),
        )
    }
    val chatColors = ChatColors(theme)
    val images = ChatThemeDetails.images

    CompositionLocalProvider(
        LocalChatColors provides chatColors,
        LocalChatShapes provides ChatShapes(),
        LocalSpace provides Space(),
        LocalImages provides images,
    ) {
        MaterialTheme(
            colors = colors,
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
    val colors: Colors
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colors

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

    /**
     * Retrieves the [Images] at the call site's position in the hierarchy.
     */
    val images: Images
        @Composable
        @ReadOnlyComposable
        get() = LocalImages.current
}
