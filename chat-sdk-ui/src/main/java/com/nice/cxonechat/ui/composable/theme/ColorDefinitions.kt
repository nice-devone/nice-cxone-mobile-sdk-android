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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.ui.graphics.Color

internal object ColorDefinitions {
    interface DefaultColors {
        val primary: Color
        val onPrimary: Color
        val background: Color
        val onBackground: Color
        val accent: Color
        val agentBackground: Color
        val agentText: Color
        val customerBackground: Color
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

    object Light: DefaultColors {
        override val primary = purple_500
        override val onPrimary = white
        override val background = white
        override val onBackground = black
        override val accent = teal_200
        override val agentBackground = light_background
        override val agentText = dark_gray_two
        override val customerBackground = cornflower_blue_two
        override val customerText = white
    }

    object Dark: DefaultColors {
        override val primary = purple_500
        override val onPrimary = white
        override val background = dark_gray_two
        override val onBackground = white
        override val accent = teal_200
        override val agentBackground = dark_background
        override val agentText = gray_light
        override val customerBackground = cornflower_blue_two
        override val customerText = white
    }
}
