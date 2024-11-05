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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.SendOutbound
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.util.UUID

@Serializable
internal data class ActionOutboundMessage(
    @SerialName("action")
    val action: EventAction = ChatWindowEvent,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("payload")
    val payload: LegacyPayload<LegacyData>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
        id: UUID,
        message: String,
        attachments: Iterable<AttachmentModel>,
        fields: List<CustomFieldModel>,
        token: String?,
    ) : this(
        payload = LegacyPayload(
                eventType = SendOutbound,
                connection = connection,
                data = LegacyData(
                    id = id,
                    thread = thread,
                    message = message,
                    attachments = attachments,
                    fields = fields,
                    token = token
                )
            )
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class LegacyData(
        @SerialName("thread")
        val thread: Thread,
        @SerialName("messageContent")
        val messageContent: MessageContent,
        @SerialName("idOnExternalPlatform")
        @Contextual
        val id: UUID,
        @SerialName("consumer")
        @JsonNames("consumer", "customer")
        val customer: CustomFieldsData? = null,
        @SerialName("consumerContact")
        @JsonNames("consumerContact", "contact")
        val customerContact: CustomFieldsData?,
        @SerialName("attachments")
        val attachments: List<AttachmentModel> = emptyList(),
        @SerialName("browserFingerprint")
        val deviceFingerprint: DeviceFingerprint = DeviceFingerprint(),
        @SerialName("accessToken")
        val accessToken: AccessTokenPayload? = null,
    ) {

        constructor(
            id: UUID,
            thread: ChatThread,
            message: String,
            attachments: Iterable<AttachmentModel>,
            fields: List<CustomFieldModel>,
            token: String?,
        ) : this(
            thread = Thread(thread),
            messageContent = MessageContent(message),
            id = id,
            customer = fields.takeUnless(List<CustomFieldModel>::isEmpty)?.let(::CustomFieldsData),
            customerContact = thread.fields.takeUnless(List<CustomField>::isEmpty)
                ?.map(::CustomFieldModel)
                ?.let(::CustomFieldsData),
            attachments = attachments.toList(),
            accessToken = token?.takeUnless(String::isBlank)?.let(::AccessTokenPayload)
        )
    }
}
