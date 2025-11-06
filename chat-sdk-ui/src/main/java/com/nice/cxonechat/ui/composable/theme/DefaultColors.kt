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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nice.cxonechat.ui.composable.theme.DefaultColors.Base
import com.nice.cxonechat.ui.composable.theme.DefaultColors.BrandPrimary
import com.nice.cxonechat.ui.composable.theme.DefaultColors.BrandSecondary
import com.nice.cxonechat.ui.composable.theme.DefaultColors.Negative
import com.nice.cxonechat.ui.composable.theme.DefaultColors.Neutral
import com.nice.cxonechat.ui.composable.theme.DefaultColors.Positive
import com.nice.cxonechat.ui.composable.theme.DefaultColors.Warning

/**
 * Provides default color palettes and theme tokens for the chat UI.
 *
 * This object contains predefined color schemes for light and dark modes,
 * as well as individual color categories such as primary, secondary, neutral,
 * positive, warning, and negative. It also includes deprecated properties for
 * backward compatibility.
 *
 * @property Base The base color palette (white and black).
 * @property BrandPrimary The primary brand color palette.
 * @property BrandSecondary The secondary brand color palette.
 * @property Neutral The neutral color palette.
 * @property Positive The positive color palette.
 * @property Warning The warning color palette.
 * @property Negative The negative color palette.
 */
object DefaultColors {
    // Expose singleton instances for use throughout the codebase (move to top for initialization order)
    val Base: Base = BaseDefault
    val BrandPrimary: BrandPrimary = BrandPrimaryDefault
    val BrandSecondary: BrandSecondary = BrandSecondaryDefault
    val Neutral: Neutral = NeutralDefault
    val Positive: Positive = PositiveDefault
    val Warning: Warning = WarningDefault
    val Negative: Negative = NegativeDefault

    // Base color definitions
    private object BaseDefault : Base {
        override val white = Color.White
        override val black = Color(0xFF030712)
    }

    /**
     * Primary colors are bold and eye-catching, perfect for drawing attention and making a strong visual statement.
     * They are often used to highlight key elements and call-to-actions in a design.
     */
    private object BrandPrimaryDefault : BrandPrimary {
        override val color50 = Color(0xFFE5EBFF)
        override val color100 = Color(0xFFCCD7FF)
        override val color200 = Color(0xFF8AA3FF)
        override val color300 = Color(0xFF6680FF)
        override val color400 = Color(0xFF3360FF)
        override val color500 = Color(0xFF254FE5)
        override val color600 = Color(0xFF0833CC)
        override val color700 = Color(0xFF0F2E99)
        override val color800 = Color(0xFF0A1E66)
        override val color900 = Color(0xFF0F1733)
        override val color950 = Color(0xFF00061A)
        override val base = color500
    }

    /**
     * Secondary colors provide additional visual interest and contrast when paired with primary colors.
     * They are useful for accentuating features and breaking up monotony in a design.
     */
    private object BrandSecondaryDefault : BrandSecondary {
        override val color50 = Color(0xFFF8FFEB)
        override val color100 = Color(0xFFF2FFD6)
        override val color200 = Color(0xFFE7FFB3)
        override val color300 = Color(0xFFDAFF8A)
        override val color400 = Color(0xFFCEFF66)
        override val color500 = Color(0xFFC1FF3D)
        override val color600 = Color(0xFFAEFF00)
        override val color700 = Color(0xFF80BD00)
        override val color800 = Color(0xFF578000)
        override val color900 = Color(0xFF2A3D00)
        override val color950 = Color(0xFF151F00)
        override val base = color500
    }

    /**
     * Neutral colors offer a subtle and sophisticated backdrop, complementing other hues without overwhelming them.
     * They are ideal for creating balance and visual rest in a design.
     */
    private object NeutralDefault : Neutral {
        override val color50 = Color(0xFFF7F8F8)
        override val color100 = Color(0xFFE6E6E8)
        override val color200 = Color(0xFFD4D5D8)
        override val color300 = Color(0xFFBFC1C6)
        override val color400 = Color(0xFFADAFB6)
        override val color500 = Color(0xFF8C8F98)
        override val color600 = Color(0xFF666A76)
        override val color700 = Color(0xFF404453)
        override val color800 = Color(0xFF333848)
        override val color900 = Color(0xFF1F2333)
        override val color950 = Color(0xFF1D2029)
        override val base = color500
    }

    /**
     * Positive colors are associated with happiness and optimism, often used to convey a cheerful, uplifting mood.
     * They are ideal for creating a friendly and welcoming atmosphere in a design.
     */
    private object PositiveDefault : Positive {
        override val color50 = Color(0xFFE5FFEE)
        override val color100 = Color(0xFFCCFFDD)
        override val color200 = Color(0xFF99FFBC)
        override val color300 = Color(0xFF80FFAB)
        override val color400 = Color(0xFF66FF9A)
        override val color500 = Color(0xFF4DFF89)
        override val color600 = Color(0xFF14CC53)
        override val color700 = Color(0xFF0F993E)
        override val color800 = Color(0xFF006623)
        override val color900 = Color(0xFF003311)
        override val color950 = Color(0xFF001A09)
        override val base = color500
    }

    /**
     * Negative colors typically signify caution or restraint, conveying a sense of seriousness or formality.
     * They are often used in designs where a sober or professional tone is required.
     */
    private object NegativeDefault : Negative {
        override val color50 = Color(0xFFFFEBEF)
        override val color100 = Color(0xFFFFD1DC)
        override val color200 = Color(0xFFFFA8BD)
        override val color300 = Color(0xFFFF7A9A)
        override val color400 = Color(0xFFFF527B)
        override val color500 = Color(0xFFFF265A)
        override val color600 = Color(0xFFE5174E)
        override val color700 = Color(0xFFAD002A)
        override val color800 = Color(0xFF75001C)
        override val color900 = Color(0xFF38000D)
        override val color950 = Color(0xFF1F0007)
        override val base = color500
    }

    /**
     * Warning colors are designed to grab attention quickly and convey urgency or importance.
     * They are most effective for alerts, error messages, or important notices in a design.
     */
    private object WarningDefault : Warning {
        override val color50 = Color(0xFFFFF8EB)
        override val color100 = Color(0xFFFFEFD1)
        override val color200 = Color(0xFFFFDFA3)
        override val color300 = Color(0xFFFFCF75)
        override val color400 = Color(0xFFFFC047)
        override val color500 = Color(0xFFFFB01A)
        override val color600 = Color(0xFFE09300)
        override val color700 = Color(0xFFA86E00)
        override val color800 = Color(0xFF704900)
        override val color900 = Color(0xFF382500)
        override val color950 = Color(0xFF1F1400)
        override val base = color500
    }

    internal val overlayBackground = Color(0x40000000)

    /**
     * Default light starting [Color] of the [Brush.linearGradient] for the accent header.
     */
    @Suppress("unused")
    @Deprecated("No longer used")
    val accentHeaderStartLight = BrandPrimary.base

    /**
     * Default dark starting [Color] of the [Brush.linearGradient] for the accent header.
     */
    @Suppress("unused")
    @Deprecated("No longer used")
    val accentHeaderStartDark = BrandPrimary.color600

    /**
     * Default light ending [Color] of the [Brush.linearGradient] for the accent header.
     */
    @Suppress("unused")
    @Deprecated("No longer used")
    val accentHeaderEndLight = Color.Unspecified

    /**
     * Default dark ending [Color] of the [Brush.linearGradient] for the accent header.
     */
    @Suppress("unused")
    @Deprecated("No longer used")
    val accentHeaderEndDark = Color.Unspecified

    /**
     * Default color palette used by the chat for the light mode.
     */
    @Deprecated("Use ThemeColorTokens and ThemeColorTokens-based palettes instead.")
    val light = ThemeColors(
        primary = BrandPrimary.base,
        onPrimary = Base.white,
        background = Neutral.color100,
        onBackground = Base.black,
        surface = Neutral.color100,
        onSurface = Base.black,
        surfaceVariant = Neutral.color200,
        surfaceContainer = Neutral.color300,
        onSurfaceHigh = Base.black,
        onSurfaceHighest = Base.black,
        accent = BrandPrimary.color800,
        onAccent = Base.white,
        agentBackground = Neutral.color200,
        agentText = Neutral.color100,
        customerBackground = BrandPrimary.color600,
        customerText = Base.white,
        agentAvatarForeground = BrandPrimary.color700,
        agentAvatarBackground = BrandPrimary.color200,
        subtle = Neutral.color50,
        muted = Neutral.color200,
        error = Negative.base,
        accentHeader = Brush.horizontalGradient(listOf(BrandPrimary.base)),
        onAccentHeader = Base.white,
        surfaceContainerHigh = Neutral.color300,
        textFieldLabelBackground = Neutral.color50,
        textFieldLabelText = Base.black,
    )

    /**
     * Default color palette used by the chat for the dark mode.
     */
    @Deprecated("Use ThemeColorTokens and ThemeColorTokens-based palettes instead.")
    val dark = ThemeColors(
        primary = BrandPrimary.color600,
        onPrimary = Base.white,
        background = Neutral.color100,
        onBackground = Base.white,
        surface = Neutral.color700,
        onSurface = Base.white,
        surfaceVariant = Neutral.color800,
        surfaceContainer = Neutral.color700,
        onSurfaceHigh = Base.white,
        onSurfaceHighest = Base.white,
        accent = BrandPrimary.color800,
        onAccent = Base.white,
        agentBackground = Neutral.color800,
        agentText = Base.white,
        customerBackground = BrandPrimary.color600,
        customerText = Base.white,
        agentAvatarForeground = BrandPrimary.color200,
        agentAvatarBackground = BrandPrimary.color700,
        subtle = Neutral.color700,
        muted = Neutral.color800,
        error = Negative.base,
        accentHeader = Brush.horizontalGradient(listOf(BrandPrimary.color600)),
        onAccentHeader = Base.white,
        surfaceContainerHigh = Neutral.color700,
        textFieldLabelBackground = Neutral.color950,
        textFieldLabelText = Base.white,
    )

    /**
     * Default ThemeColorTokens for light mode, based on Figma design system.
     *
     * This property provides a comprehensive set of color tokens for the light theme,
     * including background, content, brand, border, and status colors. The values are
     * mapped to the Figma design system and should be kept in sync with design updates.
     *
     * @see ThemeColorTokens for the structure of the token set.
     */
    val lightTokens: ThemeColorTokens = ThemeColorTokens(
        background = ThemeColorTokens.Background(
            default = Base.white,
            inverse = Base.black,
            surface = ThemeColorTokens.Background.Surface(
                default = Neutral.color100, // Figma: Surface/Container Lowest
                variant = Neutral.color200, // Figma: Surface/Container
                container = Neutral.color300, // Figma: Surface/Container High
                subtle = Neutral.color50, // Figma: Surface/Subtle
                emphasis = BrandPrimary.color50 // Figma: Brand/Primary
            )
        ),
        content = ThemeColorTokens.Content(
            primary = Base.black, // Figma: Content/Primary
            secondary = Neutral.color700, // Figma: Content/Secondary
            tertiary = Neutral.color600, // Figma: Content/Tertiary
            inverse = Base.white // Figma: Content/Inverse
        ),
        brand = ThemeColorTokens.Brand(
            primary = BrandPrimary.base, // Figma: Brand/Primary
            onPrimary = Base.white, // Figma: Brand/OnPrimary
            primaryContainer = BrandPrimary.color200, // Figma: Brand/PrimaryContainer
            onPrimaryContainer = BrandPrimary.color700, // Figma: Brand/OnPrimaryContainer
            secondary = BrandSecondary.base, // Figma: Brand/Secondary
            onSecondary = Base.black, // Figma: Brand/OnSecondary
            secondaryContainer = BrandSecondary.color100, // Figma: Brand/SecondaryContainer
            onSecondaryContainer = BrandSecondary.color900 // Figma: Brand/OnSecondaryContainer
        ),
        border = ThemeColorTokens.Border(
            default = Neutral.color200, // Figma: Border/Default
            subtle = Neutral.color100 // Figma: Border/Subtle
        ),
        status = ThemeColorTokens.Status(
            success = Positive.base, // Figma: Status/Success
            onSuccess = Positive.color950,
            successContainer = Positive.color100,
            onSuccessContainer = Positive.color900,
            warning = Warning.base, // Figma: Status/Warning
            onWarning = Warning.color900, // Figma: Status/OnWarning
            warningContainer = Warning.color100,
            onWarningContainer = Warning.color800,
            error = Negative.color600, // Figma: Status/Error
            onError = Base.white,
            errorContainer = Negative.color100,
            onErrorContainer = Negative.color950
        )
    )

    /**
     * Default ThemeColorTokens for dark mode, based on Figma design system.
     *
     * This property provides a comprehensive set of color tokens for the dark theme,
     * including background, content, brand, border, and status colors. The values are
     * mapped to the Figma design system and should be kept in sync with design updates.
     *
     * @see ThemeColorTokens for the structure of the token set.
     */
    val darkTokens: ThemeColorTokens = ThemeColorTokens(
        background = ThemeColorTokens.Background(
            default = Base.black,
            inverse = Base.white,
            surface = ThemeColorTokens.Background.Surface(
                default = Neutral.color900, // Figma: Surface/Container Lowest
                variant = Neutral.color800, // Figma: Surface/Container
                container = Neutral.color700, // Figma: Surface/Container High
                subtle = Neutral.color950, // Figma: Surface/Subtle
                emphasis = BrandPrimary.color900 // Figma: Brand/Primary
            )
        ),
        content = ThemeColorTokens.Content(
            primary = Base.white, // Figma: Content/Primary
            secondary = Neutral.color200, // Figma: Content/Secondary
            tertiary = Neutral.color400, // Figma: Content/Tertiary
            inverse = Base.black // Figma: Content/Inverse
        ),
        brand = ThemeColorTokens.Brand(
            primary = BrandPrimary.color300, // Figma: Brand/Primary
            onPrimary = Base.black, // Figma: Brand/OnPrimary
            primaryContainer = BrandPrimary.color700, // Figma: Brand/PrimaryContainer
            onPrimaryContainer = BrandPrimary.color200, // Figma: Brand/OnPrimaryContainer
            secondary = BrandSecondary.color200, // Figma: Brand/Secondary
            onSecondary = Base.black, // Figma: Brand/OnSecondary
            secondaryContainer = BrandSecondary.color900, // Figma: Brand/SecondaryContainer
            onSecondaryContainer = BrandSecondary.color100 // Figma: Brand/OnSecondaryContainer
        ),
        border = ThemeColorTokens.Border(
            default = Neutral.color800, // Figma: Border/Default
            subtle = Neutral.color900 // Figma: Border/Subtle
        ),
        status = ThemeColorTokens.Status(
            success = Positive.color300, // Figma: Status/Success
            onSuccess = Positive.color950,
            successContainer = Positive.color800,
            onSuccessContainer = Positive.color100,
            warning = Warning.color300, // Figma: Status/Warning
            onWarning = Warning.color900, // Figma: Status/OnWarning
            warningContainer = Warning.color900,
            onWarningContainer = Warning.color100,
            error = Negative.color300, // Figma: Status/Error
            onError = Negative.color900,
            errorContainer = Negative.color900,
            onErrorContainer = Negative.color100
        )
    )
}
