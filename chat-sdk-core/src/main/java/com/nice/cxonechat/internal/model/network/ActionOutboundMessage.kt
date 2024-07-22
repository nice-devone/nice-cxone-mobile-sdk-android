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
import com.nice.cxonechat.enums.EventType.SendOutbound
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

internal data class ActionOutboundMessage(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUIDProvider.next(),
    @SerializedName("payload")
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

    data class LegacyData(
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("messageContent")
        val messageContent: MessageContent,
        @SerializedName("idOnExternalPlatform")
        val id: UUID,
        @SerializedName("consumer", alternate = ["customer"])
        val customer: CustomFieldsData? = null,
        @SerializedName("consumerContact", alternate = ["contact"])
        val customerContact: CustomFieldsData?,
        @SerializedName("attachments")
        val attachments: List<AttachmentModel> = emptyList(),
        @SerializedName("browserFingerprint")
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
