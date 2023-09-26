/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.sample.ui.theme.Colors.Dark
import com.nice.cxonechat.sample.ui.theme.Colors.DefaultColors
import com.nice.cxonechat.sample.ui.theme.Colors.Light

/**
 * UI Settings as saved to file.
 *
 * @param lightModeColors Colors to be used in light mode.
 * @param darkModeColors Colors to be used in dark mode.
 * @param logo Logo image which should be used for chat branding.
 */
@Immutable
data class UISettingsModel(
    @SerializedName("lightModeColors")
    val lightModeColors: Colors = Colors(Light),
    @SerializedName("darkModeColors")
    val darkModeColors: Colors = Colors(Dark),
    @SerializedName("logo")
    val logo: String? = null,
) {
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
    data class Colors(
        @SerializedName("primary")
        val primary: Color,
        @SerializedName("onPrimary")
        val onPrimary: Color,
        @SerializedName("accent")
        val accent: Color,
        @SerializedName("onAccent")
        val onAccent: Color,
        @SerializedName("background")
        val background: Color,
        @SerializedName("onBackground")
        val onBackground: Color,
        @SerializedName("agentBubble")
        val agentBackground: Color,
        @SerializedName("agentText")
        val agentText: Color,
        @SerializedName("customerBubble")
        val customerBackground: Color,
        @SerializedName("customerText")
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
