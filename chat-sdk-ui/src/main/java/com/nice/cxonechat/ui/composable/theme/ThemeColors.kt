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
import com.nice.cxonechat.Public

/** A set of colors to be applied in either dark or light mode. */
@Suppress(
    "ComplexInterface" // Serves as definition of constants.
)
@Public
interface ThemeColors {
    /** Android primary color. */
    val primary: Color

    /** Color for icons and text displayed on [primary]. */
    val onPrimary: Color

    /** Android background color. */
    val background: Color

    /** Major color used when drawing on [background]. */
    val onBackground: Color

    /** Android accent color. */
    val accent: Color

    /** Android onAccent color. */
    val onAccent: Color

    /** Background color for agent bubbles. */
    val agentBackground: Color

    /** Color for agent text. */
    val agentText: Color

    /** Background color for customer bubbles. */
    val customerBackground: Color

    /** Color for customer text. */
    val customerText: Color

    companion object {
        /**
         * Create a set of colors for either dark or light mode.  Two sets, one for dark and
         * one for light, must be created for full colorization.
         *
         * @param primary Android primary color.
         * @param onPrimary Color for icons and text displayed on [primary].
         * @param background Android background color.
         * @param onBackground Major color used when drawing on [background].
         * @param accent Android accent color.
         * @param onAccent Android onAccent color.
         * @param agentBackground Background color for agent bubbles.
         * @param agentText Color for agent text.
         * @param customerBackground Background color for customer bubbles.
         * @param customerText Color for customer text.
         * @return Returns a properly constructed [ThemeColors] object.
         */
        @Public
        @JvmStatic
        @JvmName("create")
        @Suppress("LongParameterList")
        operator fun invoke(
            primary: Color,
            onPrimary: Color,
            background: Color,
            onBackground: Color,
            accent: Color,
            onAccent: Color,
            agentBackground: Color,
            agentText: Color,
            customerBackground: Color,
            customerText: Color,
        ): ThemeColors = ThemeColorsImpl(
            primary = primary,
            onPrimary = onPrimary,
            background = background,
            onBackground = onBackground,
            accent = accent,
            onAccent = onAccent,
            agentBackground = agentBackground,
            agentText = agentText,
            customerBackground = customerBackground,
            customerText = customerText,
        )
    }
}

@Suppress("LongParameterList")
internal data class ThemeColorsImpl(
    override val primary: Color,
    override val onPrimary: Color,
    override val background: Color,
    override val onBackground: Color,
    override val accent: Color,
    override val onAccent: Color,
    override val agentBackground: Color,
    override val agentText: Color,
    override val customerBackground: Color,
    override val customerText: Color,
) : ThemeColors
