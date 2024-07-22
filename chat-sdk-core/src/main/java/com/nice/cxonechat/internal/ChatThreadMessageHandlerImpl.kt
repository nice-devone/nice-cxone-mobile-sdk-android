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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.event.thread.LoadMoreMessagesEvent
import com.nice.cxonechat.event.thread.MessageEvent
import com.nice.cxonechat.exceptions.InvalidParameterException
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage

internal class ChatThreadMessageHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadHandler,
) : ChatThreadMessageHandler {

    override fun loadMore() {
        thread.events().trigger(LoadMoreMessagesEvent())
    }

    override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
        val uploads = message.attachments.mapNotNull(::uploadAttachment)

        // Ignore messages with no text, successful attachments, or postback.
        if (uploads.isEmpty() && message.message.isBlank() && message.postback?.isBlank() != false) {
            throw InvalidParameterException("attempt to send empty message")
        }

        val fields = chat.fields.map(::CustomFieldModel)
        val event = MessageEvent(message.message, uploads, fields, chat.storage.authToken, message.postback)
        listener?.onProcessed(event.messageId)
        thread.events().trigger(
            event = event,
            listener = {
                chat.fields = emptyList()
                listener?.onSent(event.messageId)
            },
        )
    }

    private fun uploadAttachment(attachment: ContentDescriptor): AttachmentModel? {
        val body = AttachmentUploadModel(attachment)
        val attachmentName = attachment.fileName ?: "unknown"
        val response = runCatching {
            val connection = chat.connection
            chat.service.uploadFile(
                body = body,
                brandId = connection.brandId.toString(),
                channelId = connection.channelId
            ).execute()
        }.onFailure { throwable ->
            chat.chatStateListener?.onChatRuntimeException(
                AttachmentUploadError(attachmentName, throwable)
            )
        }.getOrNull()

        return when {
            response == null -> null
            !response.isSuccessful -> {
                val err = response.errorBody()?.string().orEmpty()
                val errCode = response.code()
                chat.chatStateListener?.onChatRuntimeException(
                    AttachmentUploadError(
                        attachmentName = attachmentName,
                        cause = InvalidStateException("Error code: $errCode, error message: $err")
                    )
                )
                null
            }
            response.body()?.fileUrl == null -> {
                chat.chatStateListener?.onChatRuntimeException(
                    AttachmentUploadError(
                        attachmentName = attachmentName,
                        cause = InvalidStateException("Invalid response")
                    )
                )
                null
            }
            else -> {
                val url = response.body()?.fileUrl ?: return null
                AttachmentModel(url, attachment.friendlyName ?: "document", attachment.mimeType)
            }
        }
    }
}
