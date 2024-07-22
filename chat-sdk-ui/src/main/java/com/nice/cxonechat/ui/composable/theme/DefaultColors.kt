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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.ui.graphics.Color

internal object DefaultColors {
    private val purple_500 = Color(0xFF6200EE)
    private val teal_200 = Color(0xFF03DAC5)
    private val white = Color(0xFFFFFFFF)
    private val black = Color(0xFF000000)
    private val gray_light = Color(0xFFE8E8E8)
    private val dark_background = Color(0xFF424242)
    private val light_background = Color(0xFF03DAC6)
    private val cornflower_blue_two = Color(0xFF4F62D7)
    private val dark_gray_two = Color(0xFF191A1B)

    val light = ThemeColors(
        primary = purple_500,
        onPrimary = white,
        background = white,
        onBackground = black,
        accent = teal_200,
        onAccent = black,
        agentBackground = light_background,
        agentText = black,
        customerBackground = cornflower_blue_two,
        customerText = white,
    )

    val dark = ThemeColors(
        primary = purple_500,
        onPrimary = white,
        background = dark_gray_two,
        onBackground = white,
        accent = teal_200,
        onAccent = white,
        agentBackground = dark_background,
        agentText = gray_light,
        customerBackground = cornflower_blue_two,
        customerText = white,
    )
}
