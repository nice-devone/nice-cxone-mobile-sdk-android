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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.EditThreadNameDialog
import com.nice.cxonechat.ui.composable.conversation.attachments.SelectAttachmentsView
import com.nice.cxonechat.ui.composable.generic.ImageViewerDialogCard
import com.nice.cxonechat.ui.composable.generic.VideoView
import com.nice.cxonechat.ui.composable.theme.BusySpinner
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.CustomValues
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.EditThreadName
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.EndContact
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.ImageViewer
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.InvalidAttachments
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.None
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.Popup
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.SelectAttachments
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.VideoPlayer
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
internal fun ThreadDialogView(
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    closeChat: () -> Unit,
    threadViewModel: ChatThreadViewModel,
    chatModel: ChatViewModel,
) {
    val onDismiss: (() -> Unit) = remember { threadViewModel::dismissDialog }
    when (val dialog = threadViewModel.dialogShown.collectAsState(None).value) {
        None -> Unit
        CustomValues -> CustomValuesDialog(threadViewModel)
        EditThreadName -> EditThreadNameDialog(
            threadName = threadViewModel.selectedThreadName.orEmpty(),
            onCancel = onDismiss,
            onAccept = remember { threadViewModel::confirmEditThreadName }
        )

        is SelectAttachments -> SelectAttachmentsView(
            attachments = dialog.attachments,
            onAttachmentTapped = onAttachmentClicked,
            onShare = onShare,
            onCancel = onDismiss,
        )

        is ImageViewer -> ImageViewerDialogCard(
            image = dialog.image,
            title = dialog.title,
            onDismiss = onDismiss,
            onShare = { onShare(listOf(dialog.attachment)) },
        )

        is VideoPlayer -> VideoView(
            uri = dialog.uri,
            title = dialog.title,
            onDismiss = onDismiss,
            onShare = { onShare(listOf(dialog.attachment)) },
        )

        is InvalidAttachments -> ErrorDialog(
            title = stringResource(id = string.attachment_upload_failure),
            message = stringResource(
                string.invalid_attachments_message_template,
                dialog.attachments.map {
                    stringResource(string.invalid_attachment_template, it.value)
                }
            ),
            onDismiss = onDismiss
        )

        EndContact -> EndContactDialog(closeChat = closeChat, chatViewModel = threadViewModel, chatModel = chatModel)
        is Popup -> Popup(dialog, threadViewModel, closeChat)
    }

    if (threadViewModel.preparingToShare.collectAsState().value) {
        BusySpinner(message = stringResource(string.preparing))
    }
}
