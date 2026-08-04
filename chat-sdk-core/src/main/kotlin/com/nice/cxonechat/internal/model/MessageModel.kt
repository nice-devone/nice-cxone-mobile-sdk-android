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

import com.nice.cxonechat.Popup
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.network.CustomerStatistics
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.MessagePolyContent.ListPicker
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Noop
import com.nice.cxonechat.internal.model.network.MessagePolyContent.QuickReplies
import com.nice.cxonechat.internal.model.network.MessagePolyContent.RichLink
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text
import com.nice.cxonechat.internal.model.network.MessagePolyContent.TimePicker
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Unsupported
import com.nice.cxonechat.internal.model.network.UserStatistics
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageMetadata
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Instant

@Serializable
internal data class MessageModel(
    @SerialName("idOnExternalPlatform")
    @Contextual
    val idOnExternalPlatform: UUID,

    @SerialName("threadIdOnExternalPlatform")
    @Contextual
    val threadIdOnExternalPlatform: UUID,

    @SerialName("messageContent")
    val messageContent: MessagePolyContent,

    @SerialName("createdAtWithMilliseconds")
    @Contextual
    private val createdAtWithMilliseconds: Instant? = null,
    @SerialName("createdAt")
    @Contextual
    private val createdAtWithSeconds: Instant,

    @SerialName("attachments")
    val attachments: List<AttachmentModel>,

    @SerialName("direction")
    val direction: MessageDirectionModel,

    @SerialName("customerStatistics")
    val customerStatistics: CustomerStatistics,

    @SerialName("userStatistics")
    val userStatistics: UserStatistics,

    @SerialName("authorUser")
    val authorUser: AgentModel? = null,

    @SerialName("authorEndUserIdentity")
    val authorEndUserIdentity: CustomerIdentityModel? = null,

    @SerialName("isAiGenerated")
    val isAiGenerated: Boolean = false,
) {
    val createdAt: Instant get() = createdAtWithMilliseconds ?: createdAtWithSeconds

    val author: MessageAuthor?
        get() = when (direction) {
            ToAgent -> authorEndUserIdentity?.toMessageAuthor()
            ToClient -> authorUser?.toMessageAuthor()
        }

    val metadata: MessageMetadata
        get() = MessageMetadataInternal(
            seenAt = userStatistics.seenAt,
            readAt = userStatistics.readAt,
            seenByCustomerAt = customerStatistics.seenAt,
        )

    internal constructor(
        idOnExternalPlatform: UUID,
        threadIdOnExternalPlatform: UUID,
        messageContent: MessagePolyContent,
        createdAt: Instant,
        attachments: List<AttachmentModel>,
        direction: MessageDirectionModel,
        customerStatistics: CustomerStatistics,
        userStatistics: UserStatistics,
        authorUser: AgentModel? = null,
        authorEndUserIdentity: CustomerIdentityModel? = null,
        isAiGenerated: Boolean = false,
    ) : this(
        idOnExternalPlatform = idOnExternalPlatform,
        threadIdOnExternalPlatform = threadIdOnExternalPlatform,
        messageContent = messageContent,
        createdAtWithMilliseconds = createdAt,
        createdAtWithSeconds = createdAt,
        attachments = attachments,
        direction = direction,
        customerStatistics = customerStatistics,
        userStatistics = userStatistics,
        authorUser = authorUser,
        authorEndUserIdentity = authorEndUserIdentity,
        isAiGenerated = isAiGenerated,
    )

    fun toMessage(): Message? = when (messageContent) {
        is Text -> MessageText(this)
        is QuickReplies -> MessageQuickReplies(this)
        is ListPicker -> MessageListPicker(this)
        is RichLink -> MessageRichLink(this)
        is TimePicker -> MessageTimePicker(this)
        is MessagePolyContent.Plugin -> pluginToMessage(messageContent)
        is Unsupported -> MessageUnsupported(this, messageContent)
        is MessagePolyContent.Postback, Noop -> null
    }

    fun toPopup(): Popup? = when (messageContent) {
        is MessagePolyContent.Plugin -> {
            val rootElement = messageContent.payload?.elements.orEmpty().firstOrNull()
            if (rootElement is MessagePolyContent.Plugin.PluginElement.StructuredElements.InactivityPlugin) {
                InactivityPopupInternal(this, rootElement)
            } else {
                null
            }
        }

        else -> null
    }

    private fun pluginToMessage(messageContent: MessagePolyContent.Plugin): Message? =
        when (val rootElement = messageContent.payload?.elements.orEmpty().firstOrNull()) {
            null, is MessagePolyContent.Plugin.PluginElement.SimpleElement.Noop -> MessageUnsupported(
                model = this,
                content = Unsupported(
                    fallbackText = messageContent.fallbackText,
                    type = MessagePolyContent.Plugin.TYPE,
                    payload = null
                )
            )

            is MessagePolyContent.Plugin.PluginElement.SimpleElement.Unsupported -> MessageUnsupported(
                model = this,
                content = Unsupported(
                    fallbackText = messageContent.fallbackText,
                    type = MessagePolyContent.Plugin.TYPE,
                    payload = Unsupported.Payload(listOf(Unsupported.SubElement(type = rootElement.type)))
                )
            )
            // Only currently supported plugin element - inactivity popup - is converted to action
            else -> null
        }
}
