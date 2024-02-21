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
import com.nice.cxonechat.internal.model.network.MessagePolyElement.DeeplinkButton
import com.nice.cxonechat.internal.model.network.MessagePolyElement.IFrameButton
import com.nice.cxonechat.message.PluginElement.Button

internal data class PluginElementButton(
    private val element: MessagePolyElement.Button,
) : Button() {

    override val text: String
        get() = element.text
    override val postback: String?
        get() = element.postback
    override val deepLink: String?
        get() = when (element) {
            is DeeplinkButton -> element.url?.takeIf { it.isNotBlank() } ?: element.deepLink
            is IFrameButton -> element.url
        }
    override val displayInApp: Boolean
        get() = element is IFrameButton && deepLink != null

    override fun toString() = buildString {
        append("PluginElement.Button(text='")
        append(text)
        append("', postback='")
        append(postback)
        append("', deepLink=")
        append(deepLink)
        append(")")
    }
}
