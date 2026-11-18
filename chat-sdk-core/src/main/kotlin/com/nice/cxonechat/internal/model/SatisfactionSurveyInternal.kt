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

import com.nice.cxonechat.Popup.SatisfactionSurvey
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.IframeButtonElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.TextElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.StructuredElements.SatisfactionSurveyPlugin
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID
import kotlin.time.Instant

/**
 * Internal representation of a Satisfaction Survey popup.
 *
 * [id] is not part of the public [SatisfactionSurvey] contract - it exists for debug identification, not for
 * delivery dedup, which keys on [threadId] instead.
 */
internal data class SatisfactionSurveyInternal(
    private val model: MessageModel,
    private val popupElement: SatisfactionSurveyPlugin,
) : SatisfactionSurvey {

    val id: UUID
        get() = model.idOnExternalPlatform
    val threadId: UUID
        get() = model.threadIdOnExternalPlatform
    val createdAt: Instant
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
        val text: String,
        val surveyUrl: String,
    ) {

        /** Redacts [surveyUrl] - it can carry customer/session identifiers, and this is logged (DE-174245). */
        override fun toString() = "PopupData(text=$text, surveyUrl=<redacted>)"

        companion object {
            fun create(elements: List<SimpleElement>): PopupData {
                checkStructure(elements)
                return PopupData(
                    text = (elements[0] as TextElement).text,
                    surveyUrl = validateSurveyUrl((elements[1] as IframeButtonElement).url),
                )
            }

            private fun checkStructure(elements: List<SimpleElement>) {
                if (elements.size < 2 ||
                    elements[0] !is TextElement ||
                    elements[1] !is IframeButtonElement
                ) {
                    throw SerializationException(
                        "Invalid structure of elements for SatisfactionSurvey: " +
                                "expected [TextElement, IframeButtonElement], " +
                                "but got list of size ${elements.size} with types:" +
                                " ${elements.joinToString(", ") { it::class.simpleName ?: "Unknown" }}"
                    )
                }
            }

            /**
             * [Popup.SatisfactionSurvey.surveyUrl] is documented as HTTPS-only; reject anything else here.
             *
             * The exception message deliberately omits [url] itself - survey links can carry
             * customer/session identifiers, and this exception can propagate into logs (DE-174245).
             */
            private fun validateSurveyUrl(url: String): String {
                if (url.toHttpUrlOrNull()?.scheme != "https") {
                    throw SerializationException(
                        "Invalid survey URL for SatisfactionSurvey: expected a well-formed HTTPS URL"
                    )
                }
                return url
            }
        }
    }

    private val popupData = PopupData.create(popupElement.elements)

    override val text: String
        get() = popupData.text
    override val surveyUrl: String
        get() = popupData.surveyUrl

    override fun toString() = buildString {
        append("SatisfactionSurvey(id=")
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
