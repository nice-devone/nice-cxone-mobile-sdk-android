package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.SendMessage
import com.nice.cxonechat.enums.EventType.SendOutbound
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal data class ActionMessage constructor(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
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
        direction: MessageDirection = ToAgent,
    ) : this(
        payload = Payload(
            eventType = when (direction) {
                ToAgent -> SendMessage
                ToClient -> SendOutbound
            },
            connection = connection,
            data = Data(
                id = id,
                thread = thread,
                message = message,
                attachments = attachments,
                fields = fields,
                token = token
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
        @SerializedName("consumer")
        val consumer: CustomFieldsData? = null,
        @SerializedName("consumerContact")
        val consumerContact: CustomFieldsData?,
        @SerializedName("attachments")
        val attachments: List<AttachmentModel> = emptyList(),
        @SerializedName("browserFingerprint")
        val browserFingerprint: BrowserFingerprint = BrowserFingerprint(),
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
            consumer = fields.takeUnless { it.isEmpty() }?.let(::CustomFieldsData),
            consumerContact = thread.fields.takeUnless { it.isEmpty() }
                ?.map(::CustomFieldModel)
                ?.let(::CustomFieldsData),
            attachments = attachments.toList(),
            accessToken = token?.takeUnless { it.isBlank() }?.let(::AccessTokenPayload)
        )
    }
}
