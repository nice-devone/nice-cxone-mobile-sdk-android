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

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.Public

/** A set of colors to be applied in either dark or light mode. */
@Deprecated("Use ThemeColorTokens instead.")
@Suppress(
    "ComplexInterface" // Serves as definition of constants.
)
@Public
interface ThemeColors {
    /** Android primary color. */
    val primary: Color

    /** Color for icons and text displayed on [primary]. */
    val onPrimary: Color

    /** Android background color. */
    val background: Color

    /** Major color used when drawing on [background]. */
    val onBackground: Color

    /** Surface color. */
    val surface: Color

    /** Color for icons and text displayed on [surface]. */
    val onSurface: Color

    /** Major color variant used when drawing on surface. */
    val surfaceVariant: Color

    /** Major color used when drawing container on surface. */
    val surfaceContainer: Color

    /** Major color used when drawing on surface with high tonal elevation. */
    val surfaceContainerHigh: Color

    /** Major color used when drawing on surface with highest tonal elevation. */
    val surfaceContainerHighest: Color

    /** Android accent color. */
    val accent: Color

    /** Android onAccent color. */
    val onAccent: Color

    /** Background color for agent bubbles. */
    val agentBackground: Color

    /** Color for agent text. */
    val agentText: Color

    /** Background color for customer bubbles. */
    val customerBackground: Color

    /** Color for customer text. */
    val customerText: Color

    /** background color for position in queue panel. */
    val positionInQueueBackground: Color

    /** foreground color for position in queue panel. */
    val positionInQueueForeground: Color

    /** color for message sent icon. */
    val messageSent: Color

    /** color for message sending icon. */
    val messageSending: Color

    /** Color for agent avatar monogram displayed next to the message. */
    val agentAvatarForeground: Color

    /** Background color for agent avatar monogram displayed next to the message. */
    val agentAvatarBackground: Color

    /**
     *  Subtle background color used for extra action elements, typically a background for extra action icon next to a message,
     *  without a distinction between message author.
     **/
    val subtle: Color

    /** Color used to highlight an edge of a subtle element. */
    val muted: Color

    /** Error color. */
    val error: Color

    /** Brush used for headers emulating accent colors. */
    val accentHeader: Brush

    /** Color for icons and text displayed on [accentHeader]. */
    val onAccentHeader: Color

    /** Background color for text field label. */
    val textFieldLabelBackground: Color

    /** Text color for text field label. */
    val textFieldLabelText: Color

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        /**
         * Create a set of colors for either dark or light mode.  Two sets, one for dark and
         * one for light, must be created for full colorization.
         *
         * @param primary Android primary color.
         * @param onPrimary Color for icons and text displayed on [primary].
         * @param background Android background color.
         * @param onBackground Major color used when drawing on [background].
         * @param surface Material surface color.
         * @param onSurface Color for icons and text displayed on [surface].
         * @param surfaceVariant Major color variant used when drawing on surface (background).
         * @param surfaceContainer Major used for container on surface (background).
         * @param surfaceContainerHigh Major color used when drawing on surfaceContainer with high tonal elevation.
         * @param onSurfaceHigh Major color used when drawing on surface with high tonal elevation.
         * @param onSurfaceHighest Major color used when drawing on surface with highest tonal elevation.
         * @param accent Android accent color.
         * @param onAccent Android onAccent color.
         * @param agentBackground Background color for agent bubbles.
         * @param agentText Color for agent text.
         * @param customerBackground Background color for customer bubbles.
         * @param customerText Color for customer text.
         * @param agentAvatarForeground Color for agent avatar monogram displayed next to the message.
         * @param agentAvatarBackground Background color for agent avatar monogram displayed next to the message.
         * @param subtle Subtle background color used for extra action elements.
         * @param muted Color used to highlight an edge of a subtle element.
         * @param positionInQueueBackground color for position in queue panel.
         * @param positionInQueueForeground color for position in queue panel.
         * @param messageSent color for message sent icon.
         * @param messageSending color for message sending icon.
         * @param error Color for error content.
         * @param accentHeader Brush used for headers emulating accent colors.
         * @param onAccentHeader Color for icons and text displayed on [accentHeader].
         * @param textFieldLabelBackground Background color for text field label.
         * @param textFieldLabelText Text color for text field label.
         */
        @JvmStatic
        @JvmName("create")
        @Suppress("LongParameterList")
        operator fun invoke(
            primary: Color,
            onPrimary: Color,
            background: Color,
            onBackground: Color,
            surface: Color,
            onSurface: Color,
            surfaceVariant: Color,
            surfaceContainer: Color,
            surfaceContainerHigh: Color,
            onSurfaceHigh: Color,
            onSurfaceHighest: Color,
            accent: Color,
            onAccent: Color,
            agentBackground: Color,
            agentText: Color,
            customerBackground: Color,
            customerText: Color,
            agentAvatarForeground: Color,
            agentAvatarBackground: Color,
            subtle: Color,
            muted: Color,
            positionInQueueBackground: Color? = null,
            positionInQueueForeground: Color? = null,
            messageSent: Color = Color(0xff7f7f7f),
            messageSending: Color = Color(0xff7f7f7f),
            error: Color,
            accentHeader: Brush,
            onAccentHeader: Color,
            textFieldLabelBackground: Color,
            textFieldLabelText: Color,
        ): ThemeColors = ThemeColorsImpl(
            primary = primary,
            onPrimary = onPrimary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = onSurfaceHighest,
            accent = accent,
            onAccent = onAccent,
            agentBackground = agentBackground,
            agentText = agentText,
            customerBackground = customerBackground,
            customerText = customerText,
            agentAvatarForeground = agentAvatarForeground,
            agentAvatarBackground = agentAvatarBackground,
            positionInQueueBackground = positionInQueueBackground ?: onBackground.copy(alpha = 0.80f),
            positionInQueueForeground = positionInQueueForeground ?: background.copy(alpha = 0.80f),
            messageSent = messageSent,
            messageSending = messageSending,
            subtle = subtle,
            muted = muted,
            error = error,
            accentHeader = accentHeader,
            onAccentHeader = onAccentHeader,
            textFieldLabelBackground = textFieldLabelBackground,
            textFieldLabelText = textFieldLabelText
        )
    }
}

@Suppress("LongParameterList")
@Immutable
internal data class ThemeColorsImpl(
    override val primary: Color = Color.Unspecified,
    override val onPrimary: Color = Color.Unspecified,
    override val background: Color = Color.Unspecified,
    override val onBackground: Color = Color.Unspecified,
    override val surface: Color = Color.Unspecified,
    override val onSurface: Color = Color.Unspecified,
    override val surfaceVariant: Color = Color.Unspecified,
    override val surfaceContainer: Color = Color.Unspecified,
    override val surfaceContainerHigh: Color = Color.Unspecified,
    override val surfaceContainerHighest: Color = Color.Unspecified,
    override val accent: Color = Color.Unspecified,
    override val onAccent: Color = Color.Unspecified,
    override val agentBackground: Color = Color.Unspecified,
    override val agentText: Color = Color.Unspecified,
    override val customerBackground: Color = Color.Unspecified,
    override val customerText: Color = Color.Unspecified,
    override val positionInQueueBackground: Color = Color.Unspecified,
    override val positionInQueueForeground: Color = Color.Unspecified,
    override val messageSent: Color = Color.Unspecified,
    override val messageSending: Color = Color.Unspecified,
    override val agentAvatarForeground: Color = Color.Unspecified,
    override val agentAvatarBackground: Color = Color.Unspecified,
    override val subtle: Color = Color.Unspecified,
    override val muted: Color = Color.Unspecified,
    override val error: Color = Color.Unspecified,
    override val accentHeader: Brush = Brush.horizontalGradient(listOf(Color.Unspecified)),
    override val onAccentHeader: Color = Color.Unspecified,
    override val textFieldLabelBackground: Color = Color.Unspecified,
    override val textFieldLabelText: Color = Color.Unspecified,
) : ThemeColors

// Color palette interfaces moved from DefaultColors.kt

/**
 * Represents the base colors used in the theme.
 */
interface Base {
    /**
     * The white color used in the theme.
     */
    val white: Color

    /**
     * The black color used in the theme.
     */
    val black: Color
}

/**
 * Represents a color palette with shades ranging from light to dark.
 * Each shade is represented by a color value, allowing for a consistent and harmonious color scheme.
 * The palette includes shades from 50 (lightest) to 950 (darkest),
 * with an additional base color.
 */
@Suppress(
    "ComplexInterface" // Serves as definition of constants.
)
interface ColorShadesPalette {
    /**
     * The lightest shade in the palette (5% saturation).
     */
    val color50: Color

    /**
     * The second lightest shade in the palette (10% saturation).
     */
    val color100: Color

    /**
     * The third lightest shade in the palette (20% saturation).
     */
    val color200: Color

    /**
     * The fourth lightest shade in the palette (30% saturation).
     */
    val color300: Color

    /**
     * The fifth lightest shade in the palette (40% saturation).
     */
    val color400: Color

    /**
     * The base shade in the palette, typically the primary color (50% saturation).
     */
    val color500: Color

    /**
     * The sixth darkest shade in the palette  (60% saturation).
     */
    val color600: Color

    /**
     * The fifth darkest shade in the palette  (70% saturation).
     */
    val color700: Color

    /**
     * The fourth darkest shade in the palette (80% saturation).
     */
    val color800: Color

    /**
     * The third darkest shade in the palette (90% saturation).
     */
    val color900: Color

    /**
     * The second darkest shade in the palette (95% saturation).
     */
    val color950: Color

    /**
     * The main base color of the palette.
     */
    val base: Color
}

/**
 * Primary colors are bold and eye-catching, perfect for drawing attention and making a strong visual statement.
 * They are often used to highlight key elements and call-to-actions in a design.
 */
interface BrandPrimary : ColorShadesPalette

/**
 * Secondary colors provide additional visual interest and contrast when paired with primary colors.
 * They are useful for accentuating features and breaking up monotony in a design.
 */
interface BrandSecondary : ColorShadesPalette

/**
 * Neutral colors offer a subtle and sophisticated backdrop, complementing other hues without overwhelming them.
 * They are ideal for creating balance and visual rest in a design.
 */
interface Neutral : ColorShadesPalette

/**
 * Positive colors are associated with happiness and optimism, often used to convey a cheerful, uplifting mood.
 * They are ideal for creating a friendly and welcoming atmosphere in a design.
 */
interface Positive : ColorShadesPalette

/**
 * Negative colors typically signify caution or restraint, conveying a sense of seriousness or formality.
 * They are often used in designs where a sober or professional tone is required.
 */
interface Negative : ColorShadesPalette

/**
 * Warning colors are designed to grab attention quickly and convey urgency or importance.
 * They are most effective for alerts, error messages, or important notices in a design.
 */
interface Warning : ColorShadesPalette

@Suppress(
    "LongMethod" // Preview method
)
@PreviewLightDark
@Composable
private fun ThemeColorsList() {
    ChatTheme {
        val colors = listOf(
            "primary" to ChatTheme.colorScheme.primary,
            "onPrimary" to ChatTheme.colorScheme.onPrimary,
            "background" to ChatTheme.colorScheme.background,
            "onBackground" to ChatTheme.colorScheme.onBackground,
            "surfaceVariant" to ChatTheme.colorScheme.surfaceVariant,
            "surfaceContainer" to ChatTheme.colorScheme.surfaceContainer,
            "surfaceContainerHigh" to ChatTheme.colorScheme.surfaceContainerHigh,
            "onSurfaceHighest" to ChatTheme.colorScheme.surfaceContainerHighest,
            "secondary" to ChatTheme.colorScheme.secondary,
            "onSecondary" to ChatTheme.colorScheme.onSecondary,
            "agentBackground" to ChatTheme.chatColors.agent.background,
            "customerBackground" to ChatTheme.chatColors.customer.background,
            "leadingMessageIconBorder" to ChatTheme.chatColors.token.border.default,
            "leadingMessageIconContainer" to ChatTheme.chatColors.token.background.surface.subtle,
            "error" to ChatTheme.colorScheme.error,
        )
        val scrollState = rememberScrollState()
        Surface {
            Column(
                verticalArrangement = spacedBy(8.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {
                val rowMod = Modifier.padding(4.dp)
                val textMod = Modifier.padding(end = 8.dp)
                val shape = RoundedCornerShape(4.dp)
                val itemMod = Modifier
                    .border(2.dp, Color.Gray, shape)
                    .size(24.dp)
                colors.forEach { (label, color) ->
                    ColorPreviewItem(label, color, itemMod, rowMod, textMod, shape)
                }
                BrushPreviewItem(
                    "popupHeader",
                    Brush.horizontalGradient(listOf(ChatTheme.chatColors.token.brand.primary)),
                    itemMod,
                    rowMod,
                    textMod,
                    shape
                )
            }
        }
    }
}

@SuppressLint("ModifierParameter")
@Composable
private fun BrushPreviewItem(
    label: String,
    brush: Brush,
    itemMod: Modifier,
    rowMod: Modifier,
    textMod: Modifier,
    shape: Shape,
) {
    Row(modifier = rowMod) {
        Text(text = label, modifier = textMod)
        Spacer(modifier = itemMod.background(brush = brush, shape = shape))
    }
}

@Composable
private fun ColorPreviewItem(
    label: String,
    color: Color,
    itemMod: Modifier,
    rowMod: Modifier,
    textMod: Modifier,
    shape: Shape,
) {
    Row(modifier = rowMod) {
        Text(text = label, modifier = textMod)
        Spacer(modifier = itemMod.background(color = color, shape = shape))
    }
}
