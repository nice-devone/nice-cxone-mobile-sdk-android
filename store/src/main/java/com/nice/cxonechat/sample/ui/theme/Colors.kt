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

package com.nice.cxonechat.sample.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import com.nice.cxonechat.ui.composable.theme.DefaultColors as UIDefaultColors

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

        /** Material background color. */
        val onBackground: Color

        /** Material surface color. */
        val surface: Color

        /** Material onSurface color. */
        val onSurface: Color

        /** Material surface color. */
        val surfaceVariant: Color

        /**  Material surface color. */
        val surfaceContainer: Color

        /**  Material surface color with high tonal elevation. */
        val surfaceContainerHigh: Color

        /**  Material surface color with highest tonal elevation. */
        val surfaceContainerHighest: Color

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

        /** Color for agent avatar foreground/outline (not really app-specific, will be applied to ChatTheme). */
        val agentAvatarForeground: Color

        /** Color for agent avatar background (not really app-specific, will be applied to ChatTheme). */
        val agentAvatarBackground: Color

        /** Color for extra icons next the message, e.g. share attachments, will be applied to ChatTheme. */
        val subtle: Color

        /** Color for edge of extra icons, will be applied to ChatTheme. */
        val muted: Color

        /** Error color. */
        val error: Color

        /** Color for the header gradient start. */
        val accentHeaderStart: Color

        /** Color for the header gradient end. */
        val accentHeaderEnd: Color

        /** Color for the content on the header gradient. */
        val onAccentHeader: Color

        /** Color for text field label background. */
        val textFieldLabelBackground: Color

        /** Color for text field label text. */
        val textFieldLabelText: Color
    }

    /** default colors to use in light mode. */
    object Light : DefaultColors {
        override val primary = UIDefaultColors.light.primary
        override val onPrimary = UIDefaultColors.light.onPrimary
        override val background = UIDefaultColors.light.background
        override val onBackground = UIDefaultColors.light.onBackground
        override val surface: Color = UIDefaultColors.light.surface
        override val onSurface: Color = UIDefaultColors.light.onSurface
        override val surfaceVariant = UIDefaultColors.light.surfaceVariant
        override val surfaceContainer = UIDefaultColors.light.surfaceContainer
        override val surfaceContainerHigh: Color = UIDefaultColors.light.surfaceContainerHigh
        override val surfaceContainerHighest: Color = UIDefaultColors.light.surfaceContainerHighest
        override val accent = UIDefaultColors.light.accent
        override val onAccent = UIDefaultColors.light.onAccent
        override val agentBackground = UIDefaultColors.light.agentBackground
        override val agentText = UIDefaultColors.light.agentText
        override val agentAvatarForeground: Color = UIDefaultColors.light.agentAvatarForeground
        override val agentAvatarBackground: Color = UIDefaultColors.light.agentAvatarBackground
        override val customerBackground = UIDefaultColors.light.customerBackground
        override val customerText = UIDefaultColors.light.customerText
        override val subtle = UIDefaultColors.light.subtle
        override val muted = UIDefaultColors.light.muted
        override val error: Color = UIDefaultColors.light.error
        override val accentHeaderStart: Color = UIDefaultColors.accentHeaderStartLight
        override val accentHeaderEnd: Color = UIDefaultColors.accentHeaderEndLight
        override val onAccentHeader: Color = UIDefaultColors.light.onAccentHeader
        override val textFieldLabelBackground: Color = UIDefaultColors.light.textFieldLabelBackground
        override val textFieldLabelText: Color = UIDefaultColors.light.textFieldLabelText
    }

    /** default colors to use in dark mode. */
    object Dark : DefaultColors {
        override val primary = UIDefaultColors.dark.primary
        override val onPrimary = UIDefaultColors.dark.onPrimary
        override val background = UIDefaultColors.dark.background
        override val onBackground = UIDefaultColors.dark.onBackground
        override val surface: Color = UIDefaultColors.dark.surface
        override val onSurface: Color = UIDefaultColors.dark.onSurface
        override val surfaceVariant = UIDefaultColors.dark.surfaceVariant
        override val surfaceContainer = UIDefaultColors.dark.surfaceContainer
        override val surfaceContainerHigh: Color = UIDefaultColors.dark.surfaceContainerHigh
        override val surfaceContainerHighest: Color = UIDefaultColors.dark.surfaceContainerHighest
        override val accent = UIDefaultColors.dark.accent
        override val onAccent = UIDefaultColors.dark.onAccent
        override val agentBackground = UIDefaultColors.dark.agentBackground
        override val agentText = UIDefaultColors.dark.agentText
        override val agentAvatarForeground: Color = UIDefaultColors.dark.agentAvatarForeground
        override val agentAvatarBackground: Color = UIDefaultColors.dark.agentAvatarBackground
        override val customerBackground = UIDefaultColors.dark.customerBackground
        override val customerText = UIDefaultColors.dark.customerText
        override val subtle = UIDefaultColors.dark.subtle
        override val muted = UIDefaultColors.dark.muted
        override val error: Color = UIDefaultColors.dark.error
        override val accentHeaderStart: Color = UIDefaultColors.accentHeaderStartDark
        override val accentHeaderEnd: Color = UIDefaultColors.accentHeaderEndDark
        override val onAccentHeader: Color = UIDefaultColors.dark.onAccentHeader
        override val textFieldLabelBackground: Color = UIDefaultColors.dark.textFieldLabelBackground
        override val textFieldLabelText: Color = UIDefaultColors.dark.textFieldLabelText
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
