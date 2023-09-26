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
import com.nice.cxonechat.message.PluginElement

internal data class PluginElementSatisfactionSurvey(
    private val element: MessagePolyElement.SatisfactionSurvey,
) : PluginElement.SatisfactionSurvey() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::createPluginElement)

    override val text: Text?
        get() = pluginElements
            .filterIsInstance<Text>()
            .firstOrNull()
    override val button: Button
        get() = pluginElements
            .filterIsInstance<Button>()
            .first()
    override val postback: String?
        get() = element.postback

    override fun toString() = buildString {
        append("PluginElement.SatisfactionSurvey(text=")
        append(text)
        append(", button=")
        append(button)
        append(", postback=")
        append(postback)
        append("')")
    }

    internal companion object {
        @Suppress("KotlinConstantConditions")
        fun createVerifiedInstance(element: MessagePolyElement.SatisfactionSurvey) =
            if (element.elements.any { it is MessagePolyElement.Button }) {
                PluginElementSatisfactionSurvey(element)
            } else {
                null
            }
    }
}
