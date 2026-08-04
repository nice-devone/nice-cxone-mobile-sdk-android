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

import com.nice.cxonechat.Public

/**
 * Color values for the chat SDK.
 */
@Public
object ChatThemeDetails {
    /** Colors for dark mode. */
    @Suppress(
        "DEPRECATION", // Used for backward compatibility
        "UNUSED" // Provided for backward compatibility
    )
    @Deprecated(
        message = "Use `darkTokens` instead. Supplied values is converted to tokens on best effort basis.",
    )
    var darkColors = DefaultColors.dark
        set(value) {
            field = value
            if (value === DefaultColors.dark) {
                // Ignore default colors
                return
            }
            darkTokens = ThemeColorTokens(value)
        }

    /** Colors for light mode. */
    @Suppress(
        "DEPRECATION", // Used for backward compatibility
        "UNUSED" // Provided for backward compatibility
    )
    @Deprecated(
        message = "Use `lightTokens` instead. Supplied values is converted to tokens on best effort basis.",
    )
    var lightColors = DefaultColors.light
        set(value) {
            field = value
            if (value === DefaultColors.light) {
                // Ignore default colors
                return
            }
            lightTokens = ThemeColorTokens(value)
        }

    /**
     * The color tokens to be used for the light theme.
     */
    var lightTokens: ThemeColorTokens = DefaultColors.lightTokens

    /**
     * The color tokens to be used for the dark theme.
     */
    var darkTokens: ThemeColorTokens = DefaultColors.darkTokens
}
