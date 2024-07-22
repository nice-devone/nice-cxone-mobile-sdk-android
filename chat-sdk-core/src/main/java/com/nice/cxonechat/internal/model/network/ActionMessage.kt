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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.SendMessage
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

internal data class ActionMessage constructor(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUIDProvider.next(),
    @SerializedName("payload")
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

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("messageContent")
        val messageContent: MessageContent,
        @SerializedName("idOnExternalPlatform")
        val id: UUID,
        @SerializedName(value = "customer", alternate = ["consumer"])
        val customer: CustomFieldsData? = null,
        @SerializedName(value = "contact", alternate = ["consumerContact"])
        val customerContact: CustomFieldsData?,
        @SerializedName("attachments")
        val attachments: List<AttachmentModel> = emptyList(),
        @SerializedName("deviceFingerprint")
        val deviceFingerprint: DeviceFingerprint = DeviceFingerprint(),
        @SerializedName("accessToken")
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
