package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.event.thread.LoadMoreMessagesEvent
import com.nice.cxonechat.event.thread.MessageEvent
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import kotlin.DeprecationLevel.WARNING

internal class ChatThreadMessageHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadHandler,
) : ChatThreadMessageHandler {

    override fun loadMore() {
        thread.events().trigger(LoadMoreMessagesEvent)
    }

    @Deprecated(
        message = "Replaced in favor `send(OutboundMessage, OnMessageTransferListener)`",
        replaceWith = ReplaceWith(
            "send(message = OutboundMessage(attachments = files, message = text), listener = listener)"
        ),
        level = WARNING
    )
    override fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        listener: OnMessageTransferListener?,
    ) = send(
        OutboundMessage(
            attachments = attachments,
            message = message,
        ),
        listener
    )

    @Deprecated(
        message = "Replaced in favor `send(OutboundMessage, OnMessageTransferListener)`",
        replaceWith = ReplaceWith("send(message = OutboundMessage(message = text), listener = listener)"),
        level = WARNING
    )
    override fun send(
        message: String,
        listener: OnMessageTransferListener?,
    ) = send(
        OutboundMessage(message),
        listener = listener
    )

    override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
        val uploads = message.attachments.mapNotNull { attachment ->
            val body = AttachmentUploadModel(attachment)
            val response = chat.service.uploadFile(body, chat.connection.brandId.toString(), chat.connection.channelId).execute()
            val url = response.body()?.fileUrl ?: return@mapNotNull null
            AttachmentModel(url, attachment.friendlyName ?: "document", attachment.mimeType)
        }
        val fields = chat.fields.map(::CustomFieldModel)
        val event = MessageEvent(message.message, uploads, fields, chat.storage.authToken, message.postback)
        listener?.onProcessed(event.messageId)
        thread.events().trigger(event) {
            chat.fields = emptyList()
            listener?.onSent(event.messageId)
        }
    }
}
