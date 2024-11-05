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

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/** Default color palette used by the application. */
object Colors {
    /** Default color palette used by the application for a single mode (light or dark). */
    @Suppress(
        "ComplexInterface" // Serves as definition of constants
    )
    interface DefaultColors {
        /** Material primary color. */
        val primary: Color

        /** Material onPrimary color. */
        val onPrimary: Color

        /** Material background color. */
        val background: Color

        /** Material onBackground color. */
        val onBackground: Color

        /** Material secondary color. */
        val accent: Color

        /** Material onSecondary color. */
        val onAccent: Color

        /** Color for agent background (not really app-specific, will be applied to ChatTheme). */
        val agentBackground: Color

        /** Color for agent text (not really app-specific, will be applied to ChatTheme). */
        val agentText: Color

        /** Color for customer background (not really app-specific, will be applied to ChatTheme). */
        val customerBackground: Color

        /** Color for customer text (not really app-specific, will be applied to ChatTheme). */
        val customerText: Color
    }

    private val purple_500 = Color(0xFF6200EE)
    private val teal_200 = Color(0xFF03DAC5)
    private val white = Color(0xFFFFFFFF)
    private val black = Color(0xFF000000)
    private val gray_light = Color(0xFFe8e8e8)
    private val dark_background = Color(0xFF424242)
    private val light_background = Color(0xFFFFFFFF)
    private val cornflower_blue_two = Color(0xFF4F62D7)
    private val dark_gray_two = Color(0xFF191A1B)

    /** default colors to use in light mode. */
    object Light: DefaultColors {
        override val primary = purple_500
        override val onPrimary = white
        override val background = white
        override val onBackground = black
        override val accent = teal_200
        override val onAccent = black
        override val agentBackground = light_background
        override val agentText = dark_gray_two
        override val customerBackground = cornflower_blue_two
        override val customerText = white
    }

    /** default colors to use in dark mode. */
    object Dark: DefaultColors {
        override val primary = purple_500
        override val onPrimary = white
        override val background = dark_gray_two
        override val onBackground = white
        override val accent = teal_200
        override val onAccent = white
        override val agentBackground = dark_background
        override val agentText = gray_light
        override val customerBackground = cornflower_blue_two
        override val customerText = white
    }
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
