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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Text
import com.nice.cxonechat.message.TextFormat
import com.nice.cxonechat.message.TextFormat.Plain

internal data class PluginElementText(
    private val element: MessagePolyElement.Text,
) : Text() {

    override val text: String
        get() = element.text

    override val format = element.mimeType?.let(TextFormat::from) ?: Plain

    @Deprecated(
        "isMarkdown has been deprecated, please replace with format.",
        ReplaceWith("format.isMarkdown")
    )
    override val isMarkdown: Boolean
        get() = format.isMarkdown

    @Deprecated(
        "isHtml has been deprecated, please replace with format.",
        ReplaceWith("format.isHtml")
    )
    override val isHtml: Boolean
        get() = format.isHtml

    override fun toString() = buildString {
        append("PluginElement.Text(text='")
        append(text)
        append("', format=")
        append(format)
        append(")")
    }
}
