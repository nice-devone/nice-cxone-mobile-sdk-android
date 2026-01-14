/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.utilities.isEmpty

/**
 * Class responsible for SDK side validation that message attachments follow [com.nice.cxonechat.state.FileRestrictions],
 * before the files are actually sent to the backend.
 */
internal class ChatThreadMessageHandlerAttachmentVerification(
    private val origin: ChatThreadMessageHandler,
    private val chat: ChatWithParameters,
) : ChatThreadMessageHandler by origin {

    override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
        when (val validationResult = validateAttachments(message)) {
            is ValidationResult.Success -> origin.send(message, listener)
            is ValidationResult.Failure -> validationResult.invalidAttachments.forEach(this::notifyListener)
            // Do not send the message if there are invalid attachments.
        }
    }

    private fun validateAttachments(message: OutboundMessage): ValidationResult {
        val fileRestrictions = chat.configuration.fileRestrictions
        if (!fileRestrictions.isAttachmentsEnabled && !message.attachments.isEmpty()) {
            val failedAttachments = message.attachments.map { descriptor ->
                ValidationResult.Failure.FailedAttachment(
                    descriptor.fileName,
                    "Attachments are disabled."
                )
            }
            return ValidationResult.Failure(failedAttachments)
        }
        val allowedSize = fileRestrictions.allowedFileSize * 1024L * 1024L
        val allowedTypes = fileRestrictions.allowedFileTypes.map(AllowedFileType::mimeType).toTypedArray()
        val failedAttachments = mutableListOf<ValidationResult.Failure.FailedAttachment>()
        message.attachments.forEach { descriptor ->
            if (MimeTypeFilter.matches(descriptor.mimeType, allowedTypes) == null) {
                failedAttachments.add(
                    ValidationResult.Failure.FailedAttachment(
                        descriptor.fileName,
                        "The file has a prohibited type."
                    )
                )
            } else if (descriptor.size()?.let { it > allowedSize } == true) {
                failedAttachments.add(
                    ValidationResult.Failure.FailedAttachment(
                        descriptor.fileName,
                        "The file is too large."
                    )
                )
            }
        }
        return if (failedAttachments.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(failedAttachments)
        }
    }

    private sealed class ValidationResult {
        object Success : ValidationResult()
        data class Failure(val invalidAttachments: List<FailedAttachment>) : ValidationResult() {
            data class FailedAttachment(val filename: String, val error: String)
        }
    }

    private fun notifyListener(failedAttachment: ValidationResult.Failure.FailedAttachment) {
        val (filename, error) = failedAttachment
        chat.chatStateListener?.onChatRuntimeException(
            AttachmentUploadError(
                filename,
                IllegalArgumentException(error)
            )
        )
    }
}
