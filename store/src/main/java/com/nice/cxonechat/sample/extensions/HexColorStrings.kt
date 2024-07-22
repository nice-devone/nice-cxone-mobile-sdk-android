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

package com.nice.cxonechat.sample.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/**
 * Convert the receiver into a hex format string "#aarrggbb".
 */
val Color.asHexString get() = String.format(Locale.ROOT, "#%08x", this.toArgb())

/**
 * Convert the receiver into a hex color, parsing per android standards.
 */
val String.asHexColor get() = runCatching {
    if (this.startsWith("#")) {
        this
    } else {
        "#$this"
    }.run(android.graphics.Color::parseColor)
}.getOrNull()?.let(::Color)
