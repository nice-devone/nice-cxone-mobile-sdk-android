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
import com.nice.cxonechat.message.PluginElement.Menu

internal data class PluginElementMenu(
    private val element: MessagePolyElement.Menu,
) : Menu() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::createPluginElement)

    override val files: Iterable<File>
        get() = pluginElements
            .filterIsInstance<File>()
            .asIterable()
    override val titles: Iterable<Title>
        get() = pluginElements
            .filterIsInstance<Title>()
            .asIterable()
    override val subtitles: Iterable<Subtitle>
        get() = pluginElements
            .filterIsInstance<Subtitle>()
            .asIterable()
    override val texts: Iterable<Text>
        get() = pluginElements
            .filterIsInstance<Text>()
            .asIterable()
    override val buttons: Iterable<Button>
        get() = pluginElements
            .filterIsInstance<Button>()
            .asIterable()

    override fun toString() = buildString {
        append("PluginElement.Menu(files=")
        append(files.toList())
        append(", titles=")
        append(titles.toList())
        append(", subtitles=")
        append(subtitles.toList())
        append(", texts=")
        append(texts.toList())
        append(", buttons=")
        append(buttons.toList())
        append(")")
    }
}
