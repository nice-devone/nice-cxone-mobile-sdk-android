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
import com.nice.cxonechat.enums.EventType.SendMessage
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.util.UUID

@Serializable
internal data class ActionMessage(
    @SerialName("action")
    val action: EventAction = ChatWindowEvent,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
        id: UUID,
        message: String,
        attachments: Iterable<AttachmentModel>,
        fields: List<CustomFieldModel>,
        token: String?,
        postback: String? = null,
    ) : this(
        payload = Payload(
            eventType = SendMessage,
            connection = connection,
            data = Data(
                id = id,
                thread = thread,
                message = message,
                attachments = attachments,
                fields = fields,
                token = token,
                postback,
            )
        )
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Data(
        @SerialName("thread")
        val thread: Thread,
        @SerialName("messageContent")
        val messageContent: MessageContent,
        @SerialName("idOnExternalPlatform")
        @Contextual
        val id: UUID,
        @SerialName(value = "customer")
        @JsonNames("customer", "consumer")
        val customer: CustomFieldsData? = null,
        @SerialName(value = "contact")
        @JsonNames("contact", "consumerContact")
        val customerContact: CustomFieldsData?,
        @SerialName("attachments")
        val attachments: List<AttachmentModel> = emptyList(),
        @SerialName("deviceFingerprint")
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
            postback: String?,
        ) : this(
            thread = Thread(thread),
            messageContent = MessageContent(message, postback),
            id = id,
            customer = fields.takeUnless { it.isEmpty() }?.let(::CustomFieldsData),
            customerContact = thread.fields.takeUnless { it.isEmpty() }
                ?.map(::CustomFieldModel)
                ?.let(::CustomFieldsData),
            attachments = attachments.toList(),
            accessToken = token?.takeUnless { it.isBlank() }?.let(::AccessTokenPayload)
        )
    }
}
