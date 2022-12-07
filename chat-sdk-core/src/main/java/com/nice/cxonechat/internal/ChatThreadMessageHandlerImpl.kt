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

internal class ChatThreadMessageHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadHandler,
) : ChatThreadMessageHandler {

    override fun loadMore() {
        thread.events().trigger(LoadMoreMessagesEvent)
    }

    override fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        listener: OnMessageTransferListener?,
    ) {
        val uploads = attachments.mapNotNull { attachment ->
            val body = AttachmentUploadModel(attachment)
            val response = chat.service.uploadFile(body, chat.connection.brandId.toString(), chat.connection.channelId).execute()
            val url = response.body()?.fileUrl ?: return@mapNotNull null
            AttachmentModel(url, attachment.fileName ?: "document", attachment.mimeType)
        }
        val fields = chat.fields.map(::CustomFieldModel)
        val event = MessageEvent(message, uploads, fields, chat.storage.authToken)
        listener?.onProcessed(event.messageId)
        thread.events().trigger(event) {
            chat.fields = emptyList()
            listener?.onSent(event.messageId)
        }
    }

    override fun send(
        message: String,
        listener: OnMessageTransferListener?,
    ) = send(
        attachments = emptyList(),
        message = message,
        listener = listener
    )

}
