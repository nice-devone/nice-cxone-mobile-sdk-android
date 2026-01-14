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

package com.nice.cxonechat.ui.composable.theme

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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.Public

/** Color tokens used as a basis to setup [ChatTheme]. */
@Suppress(
    "ComplexInterface", // Serves as definition of constants.
    "LongParameterList"
)
@Public
interface ThemeColorTokens {
    /** Tokens for background colors. */
    val background: Background

    /** Tokens for content colors (e.g., text and icons). */
    val content: Content

    /** Tokens for brand-related colors. */
    val brand: Brand

    /** Tokens for border and divider colors. */
    val border: Border

    /** Tokens for system status colors (e.g., success, warning, error). */
    val status: Status

    /** Tokens used for surfaces and backgrounds of the components. */
    @Public
    interface Background {
        /**
         * The default background color for the entire app or large surfaces like cards and sheets.
         *
         * **Material 3 Token:** `md.sys.color.surface`
         *
         * **Custom Token:** `Background / Default`
         */
        val default: Color

        /**
         * A contrasting surface color used for elements that need to stand out, often for showing light content
         * on a dark background and vice versa (e.g., Snackbars, Toasts).
         *
         * **Material 3 Token:** `md.sys.color.inverse-surface`
         *
         * **Custom Token:** `Background / Inverse`
         */
        val inverse: Color

        /**
         * Tokens for the surface colors of the components.
         */
        val surface: Surface

        /** Tokens for the surface colors of the components. */
        @Public
        interface Surface {
            /**
             * A surface color with the lowest or low emphasis, often used as a base for other elements.
             * The `neutral/100` color suggests a slightly off-white matching this role.
             *
             * **Material 3 Token:** `md.sys.color.surface-container-lowest` or `md.sys.color.surface-container-low`
             *
             * **Custom Token:** `Background / Surface / Default`
             */
            val default: Color

            /**
             * The default container color. Slightly more emphasized than the main surface.
             *
             * **Material 3 Token:** `md.sys.color.surface-container`
             *
             * **Custom Token:** `Background / Surface / Variant`
             */
            val variant: Color

            /**
             * A more prominent surface color for containers that need to be clearly distinguished from the background.
             *
             * **Material 3 Token:** `md.sys.color.surface-container-high` or `md.sys.color.surface-container-highest`
             *
             * **Custom Token:** `Background / Surface / Container`
             */
            val container: Color

            /**
             * A very subtle background, often used for de-emphasized elements or large background areas that should be
             * barely visible.
             *
             * **Material 3 Token:** `md.sys.color.surface-container-lowest`
             *
             * **Custom Token:** `Background / Surface / Subtle`
             */
            val subtle: Color

            /**
             * A background color that uses the primary brand color for emphasis, typically used for highlighted
             * sections or key components.
             *
             * **Material 3 Token:** `md.sys.color.primary-container`
             *
             * **Custom Token:** `Background / Surface / Emphasis`
             */
            val emphasis: Color

            /**
             * Companion object for [Surface] interface, provides a factory method for creating [Surface] instances.
             */
            companion object {
                /**
                 * Creates a [Background.Surface] instance with the provided color values.
                 */
                @JvmStatic
                @JvmName("create")
                operator fun invoke(
                    default: Color,
                    variant: Color,
                    container: Color,
                    subtle: Color,
                    emphasis: Color,
                ): Background.Surface = BackgroundImpl.SurfaceImpl(default, variant, container, subtle, emphasis)

                /**
                 * Copies this [Background.Surface] replacing provided values.
                 *
                 * Provides an immutable copy helper for external code.
                 */
                @JvmStatic
                fun Background.Surface.copy(
                    default: Color = this.default,
                    variant: Color = this.variant,
                    container: Color = this.container,
                    subtle: Color = this.subtle,
                    emphasis: Color = this.emphasis,
                ): Background.Surface = BackgroundImpl.SurfaceImpl(
                    default = default,
                    variant = variant,
                    container = container,
                    subtle = subtle,
                    emphasis = emphasis,
                )
            }
        }

        /**
         * Companion object for [Background] interface, provides a factory method for creating [Background] instances.
         */
        companion object {
            /**
             * Creates a [Background] instance with the provided color values.
             */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(
                default: Color,
                inverse: Color,
                surface: Background.Surface,
            ): Background = BackgroundImpl(default, inverse, surface)

            /**
             * Copies this [Background] replacing provided values.
             *
             * Provides an immutable copy helper for external code.
             */
            @JvmStatic
            fun Background.copy(
                default: Color = this.default,
                inverse: Color = this.inverse,
                surface: Surface = this.surface,
            ): Background = BackgroundImpl(
                default = default,
                inverse = inverse,
                surface = surface,
            )
        }
    }

    /** Tokens used for text and icons. */
    @Public
    interface Content {
        /**
         * The primary text color used for text and icons placed on top of a `surface` color.
         *
         * **Material 3 Token:** `md.sys.color.on-surface`
         *
         * **Custom Token:** `Content / Primary`
         */
        val primary: Color

        /**
         * A secondary content color used for less prominent text and icons, like captions or helper text.
         *
         * **Material 3 Token:** `md.sys.color.on-surface-variant`
         *
         * **Custom Token:** `Content / Secondary`
         */
        val secondary: Color

        /**
         * A tertiary content color used for even less prominent text and icons, such as disabled or placeholder text.
         *
         * **Material 3 Token:** `md.sys.color.on-surface-tint`
         *
         * **Custom Token:** `Content / Tertiary`
         */
        val tertiary: Color

        /**
         * The color for text and icons placed on an `inverse-surface`.
         *
         * **Material 3 Token:** `md.sys.color.inverse-on-surface`
         *
         * **Custom Token:** `Content / Inverse`
         */
        val inverse: Color

        /**
         * Companion object for [Content] interface, provides a factory method for creating [Content] instances.
         */
        companion object {
            /**
             * Creates a [Content] instance with the provided color values.
             */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(
                primary: Color,
                secondary: Color,
                tertiary: Color,
                inverse: Color,
            ): Content = ContentImpl(primary, secondary, tertiary, inverse)

            /**
             * Copies this [Content] replacing provided values.
             *
             * Provides an immutable copy helper for external code.
             */
            @JvmStatic
            fun Content.copy(
                primary: Color = this.primary,
                secondary: Color = this.secondary,
                tertiary: Color = this.tertiary,
                inverse: Color = this.inverse,
            ): Content = ContentImpl(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                inverse = inverse,
            )
        }
    }

    /** Tokens related to your primary and secondary brand colors. */
    @Public
    interface Brand {
        /**
         * The main brand color, used for key interactive elements like buttons, active states, and FABs.
         *
         * **Material 3 Token:** `md.sys.color.primary`
         *
         * **Custom Token:** `Brand / Primary`
         */
        val primary: Color

        /**
         * Color for text and icons displayed on top of the [primary] color.
         *
         * **Material 3 Token:** `md.sys.color.on-primary`
         *
         * **Custom Token:** `Brand / OnPrimary`
         */
        val onPrimary: Color

        /**
         * A toned-down container color derived from the [primary] brand color. Used for components that need less emphasis
         * than the [primary] color.
         *
         * **Material 3 Token:** `md.sys.color.primary-container`
         *
         * **Custom Token:** `Brand / PrimaryContainer`
         */
        val primaryContainer: Color

        /**
         * Color for text and icons placed on top of the [primaryContainer] color.
         *
         * **Material 3 Token:** `md.sys.color.on-primary-container`
         *
         * **Custom Token:** `Brand / OnPrimaryContainer`
         */
        val onPrimaryContainer: Color

        /**
         * The secondary brand color, used for less prominent components that still require some accent.
         *
         * **Material 3 Token:** `md.sys.color.secondary`
         *
         * **Custom Token:** `Brand / Secondary`
         */
        val secondary: Color

        /**
         * Color for text and icons placed on top of the [secondary] color.
         *
         * **Material 3 Token:** `md.sys.color.on-secondary`
         *
         * **Custom Token:** `Brand / OnSecondary`
         */
        val onSecondary: Color

        /**
         * A toned-down container color derived from the [secondary] brand color.
         *
         * **Material 3 Token:** `md.sys.color.secondary-container`
         *
         * **Custom Token:** `Brand / SecondaryContainer`
         */
        val secondaryContainer: Color

        /**
         * Color for text and icons placed on top of the [secondaryContainer] color.
         *
         * **Material 3 Token:** `md.sys.color.on-secondary-container`
         *
         * **Custom Token:** `Brand / OnSecondaryContainer`
         */
        val onSecondaryContainer: Color

        /**
         * Companion object for [Brand] interface, provides a factory method for creating [Brand] instances.
         */
        companion object {
            /**
             * Creates a [Brand] instance with the provided color values.
             */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(
                primary: Color,
                onPrimary: Color,
                primaryContainer: Color,
                onPrimaryContainer: Color,
                secondary: Color,
                onSecondary: Color,
                secondaryContainer: Color,
                onSecondaryContainer: Color,
            ): Brand = BrandImpl(
                primary,
                onPrimary,
                primaryContainer,
                onPrimaryContainer,
                secondary,
                onSecondary,
                secondaryContainer,
                onSecondaryContainer
            )

            /**
             * Copies this [Brand] replacing provided values.
             *
             * Immutable copy helper for external usage.
             */
            @JvmStatic
            fun Brand.copy(
                primary: Color = this.primary,
                onPrimary: Color = this.onPrimary,
                primaryContainer: Color = this.primaryContainer,
                onPrimaryContainer: Color = this.onPrimaryContainer,
                secondary: Color = this.secondary,
                onSecondary: Color = this.onSecondary,
                secondaryContainer: Color = this.secondaryContainer,
                onSecondaryContainer: Color = this.onSecondaryContainer,
            ): Brand = BrandImpl(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
            )
        }
    }

    /** Tokens used for outlines and dividers. */
    @Public
    interface Border {
        /**
         * The primary border color for components like text fields and buttons, and also for dividers.
         *
         * **Material 3 Token:** `md.sys.color.outline`
         *
         * **Custom Token:** `Border / Default`
         */
        val default: Color

        /**
         * A less prominent border or divider color with lower visual contrast.
         *
         * **Material 3 Token:** `md.sys.color.outline-variant`
         *
         * **Custom Token:** `Border / Subtle`
         */
        val subtle: Color

        /**
         * Companion object for [Border] interface, provides a factory method for creating [Border] instances.
         */
        companion object {
            /**
             * Creates a [Border] instance with the provided color values.
             */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(
                default: Color,
                subtle: Color,
            ): Border = BorderImpl(default, subtle)

            /**
             * Copies this [Border] replacing provided values.
             */
            @JvmStatic
            fun Border.copy(
                default: Color = this.default,
                subtle: Color = this.subtle,
            ): Border = BorderImpl(
                default = default,
                subtle = subtle,
            )
        }
    }

    /** Tokens for communicating system status like success, warning, or error. */
    @Public
    interface Status {
        /**
         * A semantic color used to indicate a successful operation or state.
         * Not a core M3 token, but a common addition.
         *
         * **Material 3 Token:** `Success` (Custom)
         *
         * **Custom Token:** `Status / Success`
         */
        val success: Color

        /**
         * Color for text and icons displayed on top of the [success] color.
         *
         * **Material 3 Token:** `OnSuccess` (Custom)
         *
         * **Custom Token:** `Status / OnSuccess`
         */
        val onSuccess: Color

        /**
         * A toned-down container color for highlighting success states.
         *
         * **Material 3 Token:** `SuccessContainer` (Custom)
         *
         * **Custom Token:** `Status / SuccessContainer`
         */
        val successContainer: Color

        /**
         * Color for text and icons placed on top of the [successContainer] color.
         *
         * **Material 3 Token:** `OnSuccessContainer` (Custom)
         *
         * **Custom Token:** `Status / OnSuccessContainer`
         */
        val onSuccessContainer: Color

        /**
         * A semantic color used to indicate a warning or a state that requires user attention.
         * Not a core M3 token.
         *
         * **Material 3 Token:** `Warning` (Custom)
         *
         * **Custom Token:** `Status / Warning`
         */
        val warning: Color

        /**
         * Color for text and icons placed on top of the [warning] color.
         *
         * **Material 3 Token:** `OnWarning` (Custom)
         *
         * **Custom Token:** `Status / OnWarning`
         */
        val onWarning: Color

        /**
         * A toned-down container color for highlighting warning states.
         *
         * **Material 3 Token:** `WarningContainer` (Custom)
         *
         * **Custom Token:** `Status / WarningContainer`
         */
        val warningContainer: Color

        /**
         * Color for text and icons placed on top of the [warningContainer] color.
         *
         * **Material 3 Token:** `OnWarningContainer` (Custom)
         *
         * **Custom Token:** `Status / OnWarningContainer`
         */
        val onWarningContainer: Color

        /**
         * The standard M3 color for indicating an error or a failed operation.
         *
         * **Material 3 Token:** `md.sys.color.error`
         *
         * **Custom Token:** `Status / Error`
         */
        val error: Color

        /**
         * Color for text and icons displayed on top of the [error] color.
         *
         * **Material 3 Token:** `md.sys.color.on-error`
         *
         * **Custom Token:** `Status / OnError`
         */
        val onError: Color

        /**
         * A toned-down container color for highlighting error states, such as in text fields.
         *
         * **Material 3 Token:** `md.sys.color.error-container`
         *
         * **Custom Token:** `Status / ErrorContainer`
         */
        val errorContainer: Color

        /**
         * Color for text and icons placed on top of the [errorContainer] color.
         *
         * **Material 3 Token:** `md.sys.color.on-error-container`
         *
         * **Custom Token:** `Status / OnErrorContainer`
         */
        val onErrorContainer: Color

        /**
         * Companion object for [Status] interface, provides a factory method for creating [Status] instances.
         */
        companion object {
            /**
             * Creates a [Status] instance with the provided color values.
             */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(
                success: Color,
                onSuccess: Color,
                successContainer: Color,
                onSuccessContainer: Color,
                warning: Color,
                onWarning: Color,
                warningContainer: Color,
                onWarningContainer: Color,
                error: Color,
                onError: Color,
                errorContainer: Color,
                onErrorContainer: Color,
            ): Status = StatusImpl(
                success,
                onSuccess,
                successContainer,
                onSuccessContainer,
                warning,
                onWarning,
                warningContainer,
                onWarningContainer,
                error,
                onError,
                errorContainer,
                onErrorContainer
            )

            /**
             * Copies this [Status] replacing provided values.
             *
             * Immutable copy helper for external usage.
             */
            @JvmStatic
            fun Status.copy(
                success: Color = this.success,
                onSuccess: Color = this.onSuccess,
                successContainer: Color = this.successContainer,
                onSuccessContainer: Color = this.onSuccessContainer,
                warning: Color = this.warning,
                onWarning: Color = this.onWarning,
                warningContainer: Color = this.warningContainer,
                onWarningContainer: Color = this.onWarningContainer,
                error: Color = this.error,
                onError: Color = this.onError,
                errorContainer: Color = this.errorContainer,
                onErrorContainer: Color = this.onErrorContainer,
            ): Status = StatusImpl(
                success = success,
                onSuccess = onSuccess,
                successContainer = successContainer,
                onSuccessContainer = onSuccessContainer,
                warning = warning,
                onWarning = onWarning,
                warningContainer = warningContainer,
                onWarningContainer = onWarningContainer,
                error = error,
                onError = onError,
                errorContainer = errorContainer,
                onErrorContainer = onErrorContainer,
            )
        }
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {

        /**
         * Converts this [ThemeColorTokens] to a Material3 [darkColorScheme].
         */
        fun ThemeColorTokens.toDarkColorScheme() = darkColorScheme(
            primary = brand.primary,
            onPrimary = brand.onPrimary,
            background = background.default,
            onBackground = content.primary,
            surface = background.surface.default,
            onSurface = content.primary,
            surfaceVariant = background.surface.variant,
            onSurfaceVariant = content.tertiary,
            surfaceContainerLowest = background.surface.subtle,
            surfaceContainerLow = background.surface.default,
            surfaceContainer = background.surface.variant,
            surfaceContainerHigh = background.surface.container,
            surfaceContainerHighest = background.surface.container,
            secondary = brand.secondary,
            onSecondary = brand.onSecondary,
            secondaryContainer = brand.secondaryContainer,
            onSecondaryContainer = brand.onSecondaryContainer,
            primaryContainer = brand.primaryContainer,
            onPrimaryContainer = brand.onPrimaryContainer,
            error = status.error,
            onError = status.onError,
            errorContainer = status.errorContainer,
            onErrorContainer = status.onErrorContainer,
            inversePrimary = content.inverse,
            inverseSurface = background.inverse,
            inverseOnSurface = content.inverse,
            outline = border.default,
            outlineVariant = border.subtle,
            tertiary = content.tertiary,
        )

        /**
         * Converts this [ThemeColorTokens] to a Material3 [lightColorScheme].
         */
        fun ThemeColorTokens.toLightColorScheme() = lightColorScheme(
            primary = brand.primary,
            onPrimary = brand.onPrimary,
            background = background.default,
            onBackground = content.primary,
            surface = background.surface.default,
            onSurface = content.primary,
            surfaceVariant = background.surface.variant,
            onSurfaceVariant = content.tertiary,
            surfaceContainerLowest = background.surface.subtle,
            surfaceContainerLow = background.surface.default,
            surfaceContainer = background.surface.variant,
            surfaceContainerHigh = background.surface.container,
            surfaceContainerHighest = background.surface.container,
            secondary = brand.secondary,
            onSecondary = brand.onSecondary,
            secondaryContainer = brand.secondaryContainer,
            onSecondaryContainer = brand.onSecondaryContainer,
            primaryContainer = brand.primaryContainer,
            onPrimaryContainer = brand.onPrimaryContainer,
            error = status.error,
            onError = status.onError,
            errorContainer = status.errorContainer,
            onErrorContainer = status.onErrorContainer,
            inversePrimary = content.inverse,
            inverseSurface = background.inverse,
            inverseOnSurface = content.inverse,
            outline = border.default,
            outlineVariant = border.subtle,
            tertiary = content.tertiary,
        )

        /**
         * Creates a [ThemeColorTokens] instance with the provided color token groups.
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(
            background: Background,
            content: Content,
            brand: Brand,
            border: Border,
            status: Status,
        ): ThemeColorTokens = ThemeColorTokensImpl(
            background = background,
            content = content,
            brand = brand,
            border = border,
            status = status
        )

        /**
         * Copies this [ThemeColorTokens] replacing provided token groups.
         *
         * Immutable copy helper similar to data class copy().
         */
        @JvmStatic
        fun ThemeColorTokens.copy(
            background: Background = this.background,
            content: Content = this.content,
            brand: Brand = this.brand,
            border: Border = this.border,
            status: Status = this.status,
        ): ThemeColorTokens = ThemeColorTokensImpl(
            background = background,
            content = content,
            brand = brand,
            border = border,
            status = status
        )

        /**
         * Create ThemeColorTokens from ThemeColors for backward compatibility.
         *
         * @param themeColors The legacy [ThemeColors] instance to convert.
         * @return A [ThemeColorTokens] instance with mapped values.
         */
        @Deprecated("Use ThemeColorTokens directly.")
        @JvmStatic
        operator fun invoke(@Suppress("DEPRECATION") themeColors: ThemeColors): ThemeColorTokens = ThemeColorTokensImpl(
            background = BackgroundImpl(
                default = themeColors.background,
                inverse = Color.Unspecified,
                surface = BackgroundImpl.SurfaceImpl(
                    default = themeColors.surface,
                    variant = themeColors.surfaceVariant,
                    container = themeColors.surfaceContainer,
                    subtle = themeColors.subtle,
                    emphasis = themeColors.accent,
                )
            ),
            content = ContentImpl(
                primary = themeColors.onBackground,
                secondary = themeColors.onSurface,
                tertiary = themeColors.muted,
                inverse = Color.Unspecified,
            ),
            brand = BrandImpl(
                primary = themeColors.primary,
                onPrimary = themeColors.onPrimary,
                primaryContainer = themeColors.surfaceContainer,
                onPrimaryContainer = themeColors.onSurface,
                secondary = themeColors.accent,
                onSecondary = themeColors.onAccent,
                secondaryContainer = themeColors.customerBackground,
                onSecondaryContainer = themeColors.customerText,
            ),
            border = BorderImpl(
                default = themeColors.muted,
                subtle = themeColors.subtle,
            ),
            status = StatusImpl(
                success = Color.Green,
                onSuccess = Color.White,
                successContainer = Color.Green,
                onSuccessContainer = Color.White,
                warning = Color.Yellow,
                onWarning = Color.Black,
                warningContainer = Color.Yellow,
                onWarningContainer = Color.Black,
                error = themeColors.error,
                onError = Color.White,
                errorContainer = themeColors.error,
                onErrorContainer = Color.White,
            )
        )
    }
}

@Immutable
internal data class ThemeColorTokensImpl(
    override val background: ThemeColorTokens.Background,
    override val content: ThemeColorTokens.Content,
    override val brand: ThemeColorTokens.Brand,
    override val border: ThemeColorTokens.Border,
    override val status: ThemeColorTokens.Status,
) : ThemeColorTokens

@Immutable
internal data class BackgroundImpl(
    override val default: Color,
    override val inverse: Color,
    override val surface: ThemeColorTokens.Background.Surface,
) : ThemeColorTokens.Background {

    @Immutable
    data class SurfaceImpl(
        override val default: Color,
        override val variant: Color,
        override val container: Color,
        override val subtle: Color,
        override val emphasis: Color,
    ) : ThemeColorTokens.Background.Surface
}

@Immutable
internal data class ContentImpl(
    override val primary: Color,
    override val secondary: Color,
    override val tertiary: Color,
    override val inverse: Color,
) : ThemeColorTokens.Content

@Immutable
internal data class BrandImpl(
    override val primary: Color,
    override val onPrimary: Color,
    override val primaryContainer: Color,
    override val onPrimaryContainer: Color,
    override val secondary: Color,
    override val onSecondary: Color,
    override val secondaryContainer: Color,
    override val onSecondaryContainer: Color,
) : ThemeColorTokens.Brand

@Immutable
internal data class StatusImpl(
    override val success: Color,
    override val onSuccess: Color,
    override val successContainer: Color,
    override val onSuccessContainer: Color,
    override val warning: Color,
    override val onWarning: Color,
    override val warningContainer: Color,
    override val onWarningContainer: Color,
    override val error: Color,
    override val onError: Color,
    override val errorContainer: Color,
    override val onErrorContainer: Color,
) : ThemeColorTokens.Status

@Immutable
internal data class BorderImpl(
    override val default: Color,
    override val subtle: Color,
) : ThemeColorTokens.Border

@Suppress(
    "LongMethod" // Preview method
)
@Composable
@PreviewLightDark
private fun ThemeColorsList() {
    ChatTheme {
        val tokens = ChatTheme.chatColors.token
        val colors = listOf(
            // Background
            "background.default" to tokens.background.default,
            "background.inverse" to tokens.background.inverse,
            "background.surface.default" to tokens.background.surface.default,
            "background.surface.variant" to tokens.background.surface.variant,
            "background.surface.container" to tokens.background.surface.container,
            "background.surface.subtle" to tokens.background.surface.subtle,
            "background.surface.emphasis" to tokens.background.surface.emphasis,
            // Content
            "content.primary" to tokens.content.primary,
            "content.secondary" to tokens.content.secondary,
            "content.tertiary" to tokens.content.tertiary,
            "content.inverse" to tokens.content.inverse,
            // Brand
            "brand.primary" to tokens.brand.primary,
            "brand.onPrimary" to tokens.brand.onPrimary,
            "brand.primaryContainer" to tokens.brand.primaryContainer,
            "brand.onPrimaryContainer" to tokens.brand.onPrimaryContainer,
            "brand.secondary" to tokens.brand.secondary,
            "brand.onSecondary" to tokens.brand.onSecondary,
            "brand.secondaryContainer" to tokens.brand.secondaryContainer,
            "brand.onSecondaryContainer" to tokens.brand.onSecondaryContainer,
            // Border
            "border.default" to tokens.border.default,
            "border.subtle" to tokens.border.subtle,
            // Status
            "status.success" to tokens.status.success,
            "status.onSuccess" to tokens.status.onSuccess,
            "status.successContainer" to tokens.status.successContainer,
            "status.onSuccessContainer" to tokens.status.onSuccessContainer,
            "status.warning" to tokens.status.warning,
            "status.onWarning" to tokens.status.onWarning,
            "status.warningContainer" to tokens.status.warningContainer,
            "status.onWarningContainer" to tokens.status.onWarningContainer,
            "status.error" to tokens.status.error,
            "status.onError" to tokens.status.onError,
            "status.errorContainer" to tokens.status.errorContainer,
            "status.onErrorContainer" to tokens.status.onErrorContainer,
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
            }
        }
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
