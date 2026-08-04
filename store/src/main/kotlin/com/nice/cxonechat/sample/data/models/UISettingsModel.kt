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

package com.nice.cxonechat.sample.data.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nice.cxonechat.sample.ui.theme.Colors.Dark
import com.nice.cxonechat.sample.ui.theme.Colors.Light
import com.nice.cxonechat.sample.ui.theme.Images
import com.nice.cxonechat.ui.composable.theme.ThemeColorTokens
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * UI Settings as saved to file.
 *
 * @param lightModeColors Colors to be used in light mode.
 * @param darkModeColors Colors to be used in dark mode.
 * @param storedLogo Logo image which should be used for chat branding.
 */
@Immutable
@Serializable
data class UISettingsModel(
    @SerialName("lightModeColors")
    val lightModeColors: Colors = Colors(Light),
    @SerialName("darkModeColors")
    val darkModeColors: Colors = Colors(Dark),
    @SerialName("logo")
    private val storedLogo: String? = null,
) {

    /** Either stored logo, or default image. */
    val logo: Any
        get() = storedLogo ?: Images.logo

    /**
     * A set of custom colors to be applied during either day or night mode.
     */
    @Serializable
    data class Colors(
        override val content: Content,
        override val brand: Brand,
        override val border: Border,
        override val status: Status,
        override val background: Background,
    ) : ThemeColorTokens {
        /** Represents content-related colors. */
        @Serializable
        data class Content(
            @SerialName("primary")
            @Serializable(with = ColorSerializer::class)
            override val primary: Color,
            @SerialName("secondary")
            @Serializable(with = ColorSerializer::class)
            override val secondary: Color,
            @SerialName("tertiary")
            @Serializable(with = ColorSerializer::class)
            override val tertiary: Color,
            @SerialName("inverse")
            @Serializable(with = ColorSerializer::class)
            override val inverse: Color,
        ) : ThemeColorTokens.Content

        /** Represents brand-related colors. */
        @Serializable
        data class Brand(
            @SerialName("primary")
            @Serializable(with = ColorSerializer::class)
            override val primary: Color,
            @SerialName("onPrimary")
            @Serializable(with = ColorSerializer::class)
            override val onPrimary: Color,
            @SerialName("primaryContainer")
            @Serializable(with = ColorSerializer::class)
            override val primaryContainer: Color,
            @SerialName("onPrimaryContainer")
            @Serializable(with = ColorSerializer::class)
            override val onPrimaryContainer: Color,
            @SerialName("secondary")
            @Serializable(with = ColorSerializer::class)
            override val secondary: Color,
            @SerialName("onSecondary")
            @Serializable(with = ColorSerializer::class)
            override val onSecondary: Color,
            @SerialName("secondaryContainer")
            @Serializable(with = ColorSerializer::class)
            override val secondaryContainer: Color,
            @SerialName("onSecondaryContainer")
            @Serializable(with = ColorSerializer::class)
            override val onSecondaryContainer: Color,
        ) : ThemeColorTokens.Brand

        /** Represents border-related colors. */
        @Serializable
        data class Border(
            @SerialName("default")
            @Serializable(with = ColorSerializer::class)
            override val default: Color,
            @SerialName("subtle")
            @Serializable(with = ColorSerializer::class)
            override val subtle: Color,
        ) : ThemeColorTokens.Border

        /** Represents status-related colors. */
        @Serializable
        data class Status(
            @SerialName("success")
            @Serializable(with = ColorSerializer::class)
            override val success: Color,
            @SerialName("onSuccess")
            @Serializable(with = ColorSerializer::class)
            override val onSuccess: Color,
            @SerialName("successContainer")
            @Serializable(with = ColorSerializer::class)
            override val successContainer: Color,
            @SerialName("onSuccessContainer")
            @Serializable(with = ColorSerializer::class)
            override val onSuccessContainer: Color,
            @SerialName("warning")
            @Serializable(with = ColorSerializer::class)
            override val warning: Color,
            @SerialName("onWarning")
            @Serializable(with = ColorSerializer::class)
            override val onWarning: Color,
            @SerialName("warningContainer")
            @Serializable(with = ColorSerializer::class)
            override val warningContainer: Color,
            @SerialName("onWarningContainer")
            @Serializable(with = ColorSerializer::class)
            override val onWarningContainer: Color,
            @SerialName("error")
            @Serializable(with = ColorSerializer::class)
            override val error: Color,
            @SerialName("onError")
            @Serializable(with = ColorSerializer::class)
            override val onError: Color,
            @SerialName("errorContainer")
            @Serializable(with = ColorSerializer::class)
            override val errorContainer: Color,
            @SerialName("onErrorContainer")
            @Serializable(with = ColorSerializer::class)
            override val onErrorContainer: Color,
        ) : ThemeColorTokens.Status

        /** Represents background-related colors. */
        @Serializable
        data class Background(
            @SerialName("default")
            @Serializable(with = ColorSerializer::class)
            override val default: Color,
            @SerialName("inverse")
            @Serializable(with = ColorSerializer::class)
            override val inverse: Color,
            @SerialName("surface")
            override val surface: Surface,
        ) : ThemeColorTokens.Background {
            /** Represents surface-related background colors. */
            @Serializable
            data class Surface(
                @SerialName("default")
                @Serializable(with = ColorSerializer::class)
                override val default: Color,
                @SerialName("subtle")
                @Serializable(with = ColorSerializer::class)
                override val subtle: Color,
                @SerialName("variant")
                @Serializable(with = ColorSerializer::class)
                override val variant: Color,
                @SerialName("container")
                @Serializable(with = ColorSerializer::class)
                override val container: Color,
                @SerialName("emphasis")
                @Serializable(with = ColorSerializer::class)
                override val emphasis: Color,
            ) : ThemeColorTokens.Background.Surface
        }

        /**
         * A secondary constructor to initialize Colors with default values.
         *
         * @param defaults DefaultColors object containing default color values.
         */
        constructor(defaults: ThemeColorTokens) : this(
            content = Content(
                primary = defaults.content.primary,
                secondary = defaults.content.secondary,
                tertiary = defaults.content.tertiary,
                inverse = defaults.content.inverse,
            ),
            brand = Brand(
                primary = defaults.brand.primary,
                onPrimary = defaults.brand.onPrimary,
                primaryContainer = defaults.brand.primaryContainer,
                onPrimaryContainer = defaults.brand.onPrimaryContainer,
                secondary = defaults.brand.secondary,
                onSecondary = defaults.brand.onSecondary,
                secondaryContainer = defaults.brand.secondaryContainer,
                onSecondaryContainer = defaults.brand.onSecondaryContainer,
            ),
            border = Border(
                default = defaults.border.default,
                subtle = defaults.border.subtle,
            ),
            status = Status(
                success = defaults.status.success,
                onSuccess = defaults.status.onSuccess,
                successContainer = defaults.status.successContainer,
                onSuccessContainer = defaults.status.onSuccessContainer,
                warning = defaults.status.warning,
                onWarning = defaults.status.onWarning,
                warningContainer = defaults.status.warningContainer,
                onWarningContainer = defaults.status.onWarningContainer,
                error = defaults.status.error,
                onError = defaults.status.onError,
                errorContainer = defaults.status.errorContainer,
                onErrorContainer = defaults.status.onErrorContainer,
            ),
            background = Background(
                default = defaults.background.default,
                inverse = defaults.background.inverse,
                surface = Background.Surface(
                    default = defaults.background.surface.default,
                    subtle = defaults.background.surface.subtle,
                    variant = defaults.background.surface.variant,
                    container = defaults.background.surface.container,
                    emphasis = defaults.background.surface.emphasis,
                ),
            ),
        )
    }
}

/**
 * Serializer for Color class to handle serialization and deserialization.
 */
private class ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = ULong.serializer().descriptor

    override fun deserialize(decoder: Decoder): Color = ULong.serializer().deserialize(decoder).let(::Color)

    override fun serialize(encoder: Encoder, value: Color) {
        ULong.serializer().serialize(encoder, value.value)
    }
}
