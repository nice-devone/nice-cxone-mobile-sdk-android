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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.Popup.InactivityPopup
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.ButtonElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.CounterElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.TextElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.TitleElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.StructuredElements.InactivityPlugin
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import kotlinx.serialization.SerializationException
import java.util.Date
import java.util.UUID

internal data class InactivityPopupInternal(
    private val model: MessageModel,
    private val popupElement: InactivityPlugin,
) : InactivityPopup {

    val id: UUID
        get() = model.idOnExternalPlatform
    val threadId: UUID
        get() = model.threadIdOnExternalPlatform
    val createdAt: Date
        get() = model.createdAt
    val direction: MessageDirection
        get() = model.direction.toMessageDirection()
    val metadata: MessageMetadata
        get() = model.metadata
    val author: MessageAuthor?
        get() = model.author
    val attachments: Iterable<Attachment>
        get() = model.attachments.map(AttachmentModel::toAttachment)

    private data class PopupData(
        val title: String,
        val body: String,
        val countdown: Countdown,
        val callToAction: String,
        val sessionRefresh: Action,
        val sessionExpire: Action,
    ) {

        companion object {
            fun create(elements: List<SimpleElement>): PopupData {
                checkStructure(elements)
                return PopupData(
                    title = (elements[0] as TitleElement).text,
                    body = (elements[1] as TextElement).text,
                    countdown = Countdown((elements[2] as CounterElement).variables),
                    callToAction = (elements[3] as TextElement).text,
                    sessionRefresh = (elements[4] as ButtonElement).let { ActionInternal.create(it) },
                    sessionExpire = (elements[5] as ButtonElement).let { ActionInternal.create(it) },
                )
            }

            @Suppress(
                "ComplexCondition", // Structure validation is complex but necessary for the integrity of the popup data.
            )
            private fun checkStructure(elements: List<SimpleElement>) {
                if (elements.size < 6 ||
                    elements[0] !is TitleElement ||
                    elements[1] !is TextElement ||
                    elements[2] !is CounterElement ||
                    elements[3] !is TextElement ||
                    elements[4] !is ButtonElement ||
                    elements[5] !is ButtonElement
                ) {
                    throw SerializationException(
                        "Invalid structure of elements for InactivityPopup: " +
                                "expected [TitleElement, TextElement, CounterElement, TextElement, ButtonElement, ButtonElement], " +
                                "but got list of size ${elements.size} with types:" +
                                " ${elements.joinToString(", ") { it::class.simpleName ?: "Unknown" }}"
                    )
                }
            }
        }
    }

    private val popupData = PopupData.create(popupElement.elements)

    private data class Countdown(
        val variables: CounterElement.Variables,
    ) : InactivityPopup.Countdown {
        override val timeoutSeconds: Long
            get() = variables.numberOfSeconds
        override val startedAt: Date
            get() = variables.startedAt.date
    }

    override val title: String
        get() = popupData.title
    override val body: String
        get() = popupData.body
    override val countdown: InactivityPopup.Countdown
        get() = popupData.countdown
    override val callToAction: String
        get() = popupData.callToAction
    override val sessionRefresh: Action
        get() = popupData.sessionRefresh
    override val sessionExpire: Action
        get() = popupData.sessionExpire

    override fun toString() = buildString {
        append("InactivityPopup(id=")
        append(id)
        append(", threadId=")
        append(threadId)
        append(", createdAt=")
        append(createdAt)
        append(", direction=")
        append(direction)
        append(", metadata=")
        append(metadata)
        append(", author=")
        append(author)
        append(", attachments=")
        append(attachments)
        append(", popup=")
        append(popupData)
        append(")")
    }
}
