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

package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Inline formatting to be parsed from a text element.
 *
 * @property mimeType Actual mime type used.
 */
@Public
enum class TextFormat(
    val mimeType: String
) {
    /** Plain unformatted text. */
    Plain("text"),

    /** HTML formatted text. */
    Html("text/html"),

    /** Markdown formatted text. */
    Markdown("text/markdown");

    /** Returns true iff the encoding is Html. */
    val isHtml: Boolean
        get() = this === Html

    /** Returns true iff the encoding is Markdown. */
    val isMarkdown: Boolean
        get() = this === Markdown

    internal companion object {
        /**
         * Create a [TextFormat] given the proper mime type.  If there is no match, [Plain] is assumed.
         *
         * @param mimeType MimeType to decode.
         * @return Type associated with [mimeType] or [Plain] if there is no match.
         */
        fun from(mimeType: String) = entries.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) } ?: Plain
    }
}
