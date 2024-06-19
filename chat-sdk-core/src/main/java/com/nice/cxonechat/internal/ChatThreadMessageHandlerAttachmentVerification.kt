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

import androidx.core.content.MimeTypeFilter
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError
import com.nice.cxonechat.message.ContentDescriptor.Companion.size
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.state.FileRestrictions.AllowedFileType

/**
 * Class responsible for SDK side validation that message attachments follow [com.nice.cxonechat.state.FileRestrictions],
 * before the files are actually sent to the backend.
 */
internal class ChatThreadMessageHandlerAttachmentVerification(
    private val origin: ChatThreadMessageHandler,
    private val chat: ChatWithParameters,
) : ChatThreadMessageHandler by origin {

    override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
        val fileRestrictions = chat.configuration.fileRestrictions
        val allowedSize = fileRestrictions.allowedFileSize * 1024L * 1024L
        val allowedTypes = fileRestrictions.allowedFileTypes.map(AllowedFileType::mimeType).toTypedArray()
        message.attachments.forEach { descriptor ->
            if (MimeTypeFilter.matches(descriptor.mimeType, allowedTypes) == null) {
                notifyListener(
                    descriptor.fileName,
                    "The file has a prohibited type."
                )
                return
            }
            if (descriptor.size()?.let { it > allowedSize } == true) {
                notifyListener(
                    descriptor.fileName,
                    "The file is too large."
                )
                return
            }
        }
        origin.send(message, listener)
    }

    private fun notifyListener(filename: String, error: String) {
        chat.chatStateListener?.onChatRuntimeException(
            AttachmentUploadError(
                filename,
                IllegalArgumentException(error)
            )
        )
    }
}
