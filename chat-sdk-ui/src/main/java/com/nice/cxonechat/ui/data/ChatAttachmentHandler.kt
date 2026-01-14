/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.data

import android.app.Activity
import android.content.Intent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.AttachmentSharingRepository
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.ErrorGroup.LOW_SPECIFIC
import com.nice.cxonechat.ui.util.contentDescription
import com.nice.cxonechat.ui.util.isAtLeastPrepared
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Named
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scope(ChatActivity::class)
@Scoped
internal class ChatAttachmentHandler(
    private val attachmentSharingRepository: AttachmentSharingRepository,
    @Named(UiModule.LOGGER_NAME) logger: Logger,
    @InjectedParam private val lazychatThreadViewModel: Lazy<ChatThreadViewModel>,
    @InjectedParam private val chatStateViewModel: ChatStateViewModel,
) : LoggerScope by LoggerScope<ChatAttachmentHandler>(logger) {

    val chatThreadViewModel: ChatThreadViewModel by lazychatThreadViewModel

    suspend fun addAttachment(attachments: List<android.net.Uri>) = scope("addAttachment") {
        debug("Adding attachments: $attachments")
        withContext(Dispatchers.Default) {
            verbose("Checking chat state before adding attachments")
            duration {
                chatStateViewModel.state.first { it.isAtLeastPrepared() }
                verbose("Adding attachments to chat thread")
                chatThreadViewModel.addPendingAttachments(attachments)
            }
        }
    }

    suspend fun onShare(activity: Activity, attachments: Collection<Attachment>) = scope("onShare") {
        chatThreadViewModel.beginPrepareAttachments()
        withContext(Dispatchers.IO) {
            debug("Creating sharing intent for attachments: $attachments")
            val sharingIntent = attachmentSharingRepository.createSharingIntent(attachments, activity)
            chatThreadViewModel.finishPrepareAttachments()
            withContext(Dispatchers.Main) {
                if (sharingIntent == null) {
                    warning("Failed to prepare attachments for sharing")
                    chatStateViewModel.showError(LOW_SPECIFIC, activity.getString(string.prepare_attachments_failure))
                } else {
                    debug("Starting sharing activity")
                    activity.startActivity(Intent.createChooser(sharingIntent, null))
                }
            }
        }
    }

    fun onAttachmentClicked(activity: Activity, attachment: Attachment) = timedScope("onAttachmentClicked") {
        val url = attachment.url
        val mimeType = attachment.mimeType.orEmpty()
        val title = attachment.contentDescription
        when {
            mimeType.startsWith("image/") -> chatThreadViewModel.showImage(
                image = url,
                title = title.takeUnless { it.isNullOrEmpty() } ?: activity.getString(string.image_preview_title),
                attachment = attachment
            )

            mimeType.startsWith("video/") -> chatThreadViewModel.showVideo(
                url = url,
                title = title ?: activity.getString(string.video_preview_title),
                attachment = attachment
            )

            else -> openWithAndroid(activity, attachment)
        }
    }

    private fun openWithAndroid(activity: Activity, attachment: Attachment) = scope("openWithAndroid") {
        debug("Opening attachment with Android: $attachment")
        if (!activity.openWithAndroid(attachment.url, attachment.mimeType)) {
            warning("No application found to open attachment with mime type: ${attachment.mimeType}")
            chatStateViewModel.showError(
                errorGroup = LOW_SPECIFIC,
                message = activity.getString(string.unsupported_type_message, attachment.mimeType),
                title = activity.getString(string.unsupported_type_title)
            )
        }
    }
}
