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
     * @param background Material background color.
     * @param onBackground Material onBackground color.
     * @param agentBackground Background color for agent cells in chat.
     * @param agentText Text color for agent cells in chat.
     * @param customerBackground Background color for customer cells in chat.
     * @param customerText Text color for customer cells in chat.
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
        @SerialName("agentBubble")
        @Serializable(with = ColorSerializer::class)
        val agentBackground: Color,
        @SerialName("agentText")
        @Serializable(with = ColorSerializer::class)
        val agentText: Color,
        @SerialName("customerBubble")
        @Serializable(with = ColorSerializer::class)
        val customerBackground: Color,
        @SerialName("customerText")
        @Serializable(with = ColorSerializer::class)
        val customerText: Color,
    ) {
        constructor(defaults: DefaultColors) : this(
            primary = defaults.primary,
            onPrimary = defaults.onPrimary,
            accent = defaults.accent,
            onAccent = defaults.onAccent,
            background = defaults.background,
            onBackground = defaults.onBackground,
            agentBackground = defaults.agentBackground,
            agentText = defaults.agentText,
            customerBackground = defaults.customerBackground,
            customerText = defaults.customerText
        )
    }
}

private class ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = ULong.serializer().descriptor

    override fun deserialize(decoder: Decoder): Color = ULong.serializer().deserialize(decoder).let(::Color)

    override fun serialize(encoder: Encoder, value: Color) {
        ULong.serializer().serialize(encoder, value.value)
    }
}
