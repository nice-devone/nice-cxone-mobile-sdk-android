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
import com.nice.cxonechat.message.PluginElement.InactivityPopup

internal data class PluginElementInactivityPopup(
    private val element: MessagePolyElement.InactivityPopup,
) : InactivityPopup() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::createPluginElement)

    override val title: Title
        get() = pluginElements
            .filterIsInstance<Title>()
            .first()
    override val subtitle: Subtitle?
        get() = pluginElements
            .filterIsInstance<Subtitle>()
            .firstOrNull()
    override val texts: Iterable<Text>
        get() = pluginElements
            .filterIsInstance<Text>()
            .asIterable()
    override val buttons: Iterable<Button>
        get() = pluginElements
            .filterIsInstance<Button>()
            .asIterable()
    override val countdown: Countdown
        get() = pluginElements
            .filterIsInstance<Countdown>()
            .first()

    override fun toString() = buildString {
        append("PluginElement.InactivityPopup(title=")
        append(title)
        append(", texts=")
        append(texts.toList())
        append(", buttons=")
        append(buttons.toList())
        append(", countdown=")
        append(countdown)
        append(")")
    }
}
