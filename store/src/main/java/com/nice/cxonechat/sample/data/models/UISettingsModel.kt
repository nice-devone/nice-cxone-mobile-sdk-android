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

package com.nice.cxonechat.sample.data.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nice.cxonechat.sample.ui.theme.Colors.Dark
import com.nice.cxonechat.sample.ui.theme.Colors.DefaultColors
import com.nice.cxonechat.sample.ui.theme.Colors.Light
import com.nice.cxonechat.sample.ui.theme.Images
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
     *
     * @param primary Material primary color.
     * @param onPrimary Material onPrimary color.
     * @param accent Material secondary color.
     * @param onAccent Material onSecondary color.
     * @param background Material background and surface color.
     * @param onBackground Material onBackground and onSurface color.
     * @param surface Material surface color.
     * @param onSurface Material on-surface color.
     * @param surfaceVariant Material surface variant color.
     * @param surfaceContainer Material onSurface color.
     * @param surfaceContainerHigh Material onSurface color with high tonal elevation.
     * @param surfaceContainerHighest Material onSurface color with highest tonal elevation.
     * @param agentBackground Background color for agent cells in chat.
     * @param agentText Text color for agent cells in chat.
     * @param agentAvatarForeground Color for agent avatar outline in chat conversation.
     * @param agentAvatarBackground Color for agent avatar background in chat conversation.
     * @param customerBackground Background color for customer cells in chat.
     * @param customerText Text color for customer cells in chat.
     * @param subtle Subtle color for less prominent UI elements, e.g. action to download all attachments.
     * @param muted Muted color for disabled or less important UI elements, e.g. a rim for subtle element.
     * @param error Color for error content.
     * @param accentHeaderStart Starting color for the gradient in the header.
     * @param accentHeaderEnd Ending color for the gradient in the header.
     * @param onAccentHeader Content color for the header gradient.
     * @param textFieldLabelBackground Background color for text field label.
     * @param textFieldLabelText Text color for text field label.
     */
    @Serializable
    data class Colors(
        @SerialName("primary")
        @Serializable(with = ColorSerializer::class)
        val primary: Color,
        @SerialName("onPrimary")
        @Serializable(with = ColorSerializer::class)
        val onPrimary: Color,
        @SerialName("accent")
        @Serializable(with = ColorSerializer::class)
        val accent: Color,
        @SerialName("onAccent")
        @Serializable(with = ColorSerializer::class)
        val onAccent: Color,
        @SerialName("background")
        @Serializable(with = ColorSerializer::class)
        val background: Color,
        @SerialName("onBackground")
        @Serializable(with = ColorSerializer::class)
        val onBackground: Color,
        @SerialName("surface")
        @Serializable(with = ColorSerializer::class)
        val surface: Color,
        @SerialName("onSurface")
        @Serializable(with = ColorSerializer::class)
        val onSurface: Color,
        @SerialName("surfaceVariant")
        @Serializable(with = ColorSerializer::class)
        val surfaceVariant: Color,
        @SerialName("surfaceContainer")
        @Serializable(with = ColorSerializer::class)
        val surfaceContainer: Color,
        @SerialName("surfaceContainerHigh")
        @Serializable(with = ColorSerializer::class)
        val surfaceContainerHigh: Color,
        @SerialName("surfaceContainerHighest")
        @Serializable(with = ColorSerializer::class)
        val surfaceContainerHighest: Color,
        @SerialName("agentBubble")
        @Serializable(with = ColorSerializer::class)
        val agentBackground: Color,
        @SerialName("agentText")
        @Serializable(with = ColorSerializer::class)
        val agentText: Color,
        @SerialName("agentAvatarOutline")
        @Serializable(with = ColorSerializer::class)
        val agentAvatarForeground: Color,
        @SerialName("agentAvatarBackground")
        @Serializable(with = ColorSerializer::class)
        val agentAvatarBackground: Color,
        @SerialName("customerBubble")
        @Serializable(with = ColorSerializer::class)
        val customerBackground: Color,
        @SerialName("customerText")
        @Serializable(with = ColorSerializer::class)
        val customerText: Color,
        @SerialName("subtle")
        @Serializable(with = ColorSerializer::class)
        val subtle: Color,
        @SerialName("muted")
        @Serializable(with = ColorSerializer::class)
        val muted: Color,
        @SerialName("error")
        @Serializable(with = ColorSerializer::class)
        val error: Color,
        @SerialName("accentHeaderStart")
        @Serializable(with = ColorSerializer::class)
        val accentHeaderStart: Color,
        @SerialName("accentHeaderEnd")
        @Serializable(with = ColorSerializer::class)
        val accentHeaderEnd: Color,
        @SerialName("onAccentHeader")
        @Serializable(with = ColorSerializer::class)
        val onAccentHeader: Color,
        @SerialName("textFieldLabelBackground")
        @Serializable(with = ColorSerializer::class)
        val textFieldLabelBackground: Color,
        @SerialName("textFieldLabelText")
        @Serializable(with = ColorSerializer::class)
        val textFieldLabelText: Color,
    ) {

        /**
         * A secondary constructor to initialize Colors with default values.
         *
         * @param defaults DefaultColors object containing default color values.
         */
        constructor(defaults: DefaultColors) : this(
            primary = defaults.primary,
            onPrimary = defaults.onPrimary,
            accent = defaults.accent,
            onAccent = defaults.onAccent,
            background = defaults.background,
            onBackground = defaults.onBackground,
            surface = defaults.surface,
            onSurface = defaults.onSurface,
            surfaceVariant = defaults.surfaceVariant,
            surfaceContainer = defaults.surfaceContainer,
            surfaceContainerHigh = defaults.surfaceContainerHigh,
            surfaceContainerHighest = defaults.surfaceContainerHighest,
            agentBackground = defaults.agentBackground,
            agentText = defaults.agentText,
            agentAvatarForeground = defaults.agentAvatarForeground,
            agentAvatarBackground = defaults.agentAvatarBackground,
            customerBackground = defaults.customerBackground,
            customerText = defaults.customerText,
            subtle = defaults.subtle,
            muted = defaults.muted,
            error = defaults.error,
            accentHeaderStart = defaults.accentHeaderStart,
            accentHeaderEnd = defaults.accentHeaderEnd,
            onAccentHeader = defaults.onAccentHeader,
            textFieldLabelBackground = defaults.textFieldLabelBackground,
            textFieldLabelText = defaults.textFieldLabelText
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
