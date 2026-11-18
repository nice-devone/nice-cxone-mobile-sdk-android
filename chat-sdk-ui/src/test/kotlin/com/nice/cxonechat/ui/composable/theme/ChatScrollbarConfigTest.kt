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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the scrollbar appearance against the design it was taken from.
 *
 * The two colour assertions carry the actual design values. `Color/Content/Tertiary` resolves to
 * `#666A76` in the light design and `#ADAFB6` in the dark one, and those are exactly the two modes of
 * the theme's own `content.tertiary` token — which is why the config binds to the token instead of
 * naming either literal. If a redesign moves the token, these two assertions are what notice.
 */
class ChatScrollbarConfigTest {

    private val space = Space()

    @Test
    fun `the indicator takes the light tertiary content colour from the design`() {
        val config = chatScrollbarConfig(DefaultColors.lightTokens, space)

        assertEquals(Color(0xFF666A76), config.indicatorColor)
        assertEquals(DefaultColors.lightTokens.content.tertiary, config.indicatorColor)
    }

    @Test
    fun `the indicator takes the dark tertiary content colour from the design`() {
        val config = chatScrollbarConfig(DefaultColors.darkTokens, space)

        assertEquals(Color(0xFFADAFB6), config.indicatorColor)
        assertEquals(DefaultColors.darkTokens.content.tertiary, config.indicatorColor)
    }

    @Test
    fun `the design shows no track, so the bar is transparent and never drawn`() {
        val config = chatScrollbarConfig(DefaultColors.lightTokens, space)

        assertEquals(Color.Transparent, config.barColor)
        assertEquals(0f, config.barColor.alpha, 0f)
    }

    @Test
    fun `both dimensions are taken from the theme's spacing rather than written inline`() {
        // Overriding the spacing has to move the scrollbar; a literal in the builder would not follow.
        val doubled = Space(scrollbarThickness = 8.dp, scrollbarInset = 12.dp)

        val config = chatScrollbarConfig(DefaultColors.lightTokens, doubled)

        assertEquals(8.dp, config.indicatorThickness)
        assertEquals(8.dp, config.barThickness)
        assertEquals(12.dp, config.padding.calculateTopPadding())
        assertEquals(12.dp, config.padding.calculateRightPadding(Ltr))
        // Half the thickness is what keeps a bar this thin reading as a pill rather than a rectangle.
        assertEquals(4.dp, config.indicatorCornerRadius)
    }
}
