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
import com.nice.cxonechat.ui.viewmodel.ConversationDialog
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
    val onDismiss = remember { threadViewModel::dismissDialog }
    val dialog = threadViewModel.dialogShown.collectAsState(None).value

    ShowDialog(
        dialog = dialog,
        onAttachmentClicked = onAttachmentClicked,
        onShare = onShare,
        closeChat = closeChat,
        threadViewModel = threadViewModel,
        chatModel = chatModel,
        onDismiss = onDismiss
    )

    if (threadViewModel.preparingToShare.collectAsState().value) {
        BusySpinner(message = stringResource(string.preparing))
    }
}

@Composable
private fun ShowDialog(
    dialog: ConversationDialog,
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    closeChat: () -> Unit,
    threadViewModel: ChatThreadViewModel,
    chatModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        None -> Unit
        CustomValues -> CustomValuesDialog(threadViewModel)
        EditThreadName -> EditThreadNameDialogWrapper(threadViewModel, onDismiss)
        EndContact -> EndContactDialog(closeChat = closeChat, chatViewModel = threadViewModel, chatModel = chatModel)
        is SelectAttachments -> SelectAttachmentsDialog(dialog, onAttachmentClicked, onShare, onDismiss)
        is ImageViewer -> ImageViewerDialogWrapper(dialog, onDismiss, onShare)
        is VideoPlayer -> VideoPlayerDialogWrapper(dialog, onDismiss, onShare)
        is InvalidAttachments -> InvalidAttachmentsDialog(dialog, onDismiss)
        is Popup -> Popup(dialog, threadViewModel, closeChat)
    }
}

@Composable
private fun EditThreadNameDialogWrapper(
    threadViewModel: ChatThreadViewModel,
    onDismiss: () -> Unit,
) {
    EditThreadNameDialog(
        threadName = threadViewModel.selectedThreadName.orEmpty(),
        onCancel = onDismiss,
        onAccept = remember { threadViewModel::confirmEditThreadName }
    )
}

@Composable
private fun SelectAttachmentsDialog(
    dialog: SelectAttachments,
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectAttachmentsView(
        attachments = dialog.attachments,
        onAttachmentTapped = onAttachmentClicked,
        onShare = onShare,
        onCancel = onDismiss,
    )
}

@Composable
private fun ImageViewerDialogWrapper(
    dialog: ImageViewer,
    onDismiss: () -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    ImageViewerDialogCard(
        image = dialog.image,
        title = dialog.title,
        onDismiss = onDismiss,
        onShare = { onShare(listOf(dialog.attachment)) },
    )
}

@Composable
private fun VideoPlayerDialogWrapper(
    dialog: VideoPlayer,
    onDismiss: () -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    VideoView(
        uri = dialog.uri,
        title = dialog.title,
        onDismiss = onDismiss,
        onShare = { onShare(listOf(dialog.attachment)) },
    )
}

@Composable
private fun InvalidAttachmentsDialog(
    dialog: InvalidAttachments,
    onDismiss: () -> Unit,
) {
    ErrorDialog(
        title = stringResource(id = string.attachment_upload_failure),
        message = stringResource(
            string.invalid_attachments_message_template,
            dialog.attachments.map {
                stringResource(string.invalid_attachment_template, it.value)
            }
        ),
        onDismiss = onDismiss
    )
}
