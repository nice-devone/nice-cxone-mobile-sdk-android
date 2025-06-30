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

/**
 * Chat UI default colors.
 */
object DefaultColors {

    private val white = Color(0xFFFFFFFF)
    private val grey100 = Color(0xFF131214)
    private val grey80 = Color(0xFF2F3133)
    private val grey70 = Color(0xFF1F2224)
    private val grey50 = Color(0xFF40484B)
    private val grey30 = Color(0xFFF4F6F7)
    private val grey20 = Color(0xFFE6E9EB)
    private val brand90 = Color(0xFF013B72)
    private val brand80 = Color(0xFF014D93)
    private val brand60 = Color(0xFF016CCF)
    private val brand20 = Color(0xFFB0D1F0)
    private val accentBoldLight = Color(0xFF0162BC)
    private val accentBoldDark = Color(0xFF559DDF)
    private val accentMutedLight = Color(0xFF8ABBE9)
    private val accentMutedDark = Color(0xFF014D93)
    private val surface = Color(0xFFFFFFFF)
    private val surfaceLight = Color(0xFFF5FAFD)
    private val surfaceVariant = Color(0xFFF5FAFD)
    private val surfaceVariantDark = Color(0xFF43474E)
    private val surfaceContainerLight = Color(0xFFEAEFF1)
    private val surfaceContainerHigh = Color(0xFFE6E9EB)
    private val surfaceContainerDark = Color(0xFF1B2023)
    private val onSurfaceHighLight = Color(0xFFE6E9EB)
    private val onSurfaceHighDark = Color(0xFF282A2F)
    private val onSurfaceHighestLight = Color(0xFFDEE3E6)
    private val onSurfaceHighestDark = Color(0xFF33353A)
    private val error = Color(0xFFDB340B)

    internal val danger = Color(0xFFDB340B)

    internal val pop = Color(0xFF2FD3FF)

    private val accentPrimaryLight = brand20
    private val accentPrimaryDark = brand90

    internal val staticDark = Color(0xFF131214)
    internal val overlayBackground = Color(0x40000000)

    /**
     * Default light starting [Color] of the [Brush.linearGradient] for the accent header.
     */
    val accentHeaderStartLight = accentBoldLight

    /**
     * Default dark starting [Color] of the [Brush.linearGradient] for the accent header.
     */
    val accentHeaderStartDark = accentBoldDark

    /**
     * Default light ending [Color] of the [Brush.linearGradient] for the accent header.
     */
    val accentHeaderEndLight = pop

    /**
     * Default dark ending [Color] of the [Brush.linearGradient] for the accent header.
     */
    val accentHeaderEndDark = pop

    /**
     * Default color palette used by the chat for the light mode.
     */
    val light = ThemeColors(
        primary = brand60,
        onPrimary = white,
        background = white,
        onBackground = grey100,
        surface = surface,
        onSurface = grey100,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceContainerLight,
        onSurfaceHigh = onSurfaceHighLight,
        onSurfaceHighest = onSurfaceHighestLight,
        accent = brand80,
        onAccent = white,
        agentBackground = grey20,
        agentText = grey100,
        customerBackground = brand60,
        customerText = white,
        agentAvatarForeground = accentBoldLight,
        agentAvatarBackground = accentMutedLight,
        subtle = grey30,
        muted = grey20,
        error = error,
        accentHeader = Brush.horizontalGradient(listOf(accentHeaderStartLight, accentHeaderEndDark)),
        onAccentHeader = accentPrimaryLight,
        surfaceContainerHigh = surfaceContainerHigh,
        textFieldLabelBackground = surfaceLight,
        textFieldLabelText = grey50,
    )

    /**
     * Default color palette used by the chat for the dark mode.
     */
    val dark = ThemeColors(
        primary = brand60,
        onPrimary = white,
        background = grey100,
        onBackground = white,
        surface = grey70,
        onSurface = white,
        surfaceVariant = surfaceVariantDark,
        surfaceContainer = surfaceContainerDark,
        onSurfaceHigh = onSurfaceHighDark,
        onSurfaceHighest = onSurfaceHighestDark,
        accent = brand80,
        onAccent = white,
        agentBackground = grey80,
        agentText = white,
        customerBackground = brand60,
        customerText = white,
        agentAvatarForeground = accentBoldDark,
        agentAvatarBackground = accentMutedDark,
        subtle = grey70,
        muted = grey80,
        error = error,
        accentHeader = Brush.horizontalGradient(listOf(accentHeaderStartDark, accentHeaderEndDark)),
        onAccentHeader = accentPrimaryDark,
        surfaceContainerHigh = surfaceContainerHigh,
        textFieldLabelBackground = surfaceLight,
        textFieldLabelText = grey50,
    )
}
