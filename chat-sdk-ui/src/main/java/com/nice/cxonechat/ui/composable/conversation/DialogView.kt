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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.EditThreadNameDialog
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.generic.ImageViewerDialogCard
import com.nice.cxonechat.ui.composable.generic.VideoViewerDialogCard
import com.nice.cxonechat.ui.composable.theme.BusySpinner
import com.nice.cxonechat.ui.main.ChatThreadViewModel
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.AudioPlayer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.EndContact
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorAttachmentNotSupported
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorAttachmentTooLarge
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorUnableToReadAttachment
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ImageViewer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.SelectAttachments
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.VideoPlayer
import com.nice.cxonechat.ui.main.ChatViewModel

@Composable
internal fun DialogView(
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    closeChat: () -> Unit,
    threadViewModel: ChatThreadViewModel,
    chatModel: ChatViewModel,
) {
    when (val dialog = threadViewModel.dialogShown.collectAsState(None).value) {
        None -> Unit
        CustomValues -> CustomValuesDialog(threadViewModel)
        EditThreadName -> EditThreadNameDialog(
            threadName = threadViewModel.selectedThreadName.orEmpty(),
            onCancel = threadViewModel::dismissDialog,
            onAccept = threadViewModel::confirmEditThreadName
        )

        is AudioPlayer -> AudioPlayerDialog(
            url = dialog.url,
            title = dialog.title,
            onCancel = threadViewModel::dismissDialog,
        )

        is SelectAttachments -> SelectAttachmentsDialog(
            attachments = dialog.attachments,
            title = dialog.title.orEmpty(),
            onAttachmentTapped = onAttachmentClicked,
            onCancel = threadViewModel::dismissDialog,
            onShare = onShare,
        )

        is ImageViewer -> ImageViewerDialogCard(
            image = dialog.image,
            title = dialog.title,
            onDismiss = threadViewModel::dismissDialog,
        )

        is VideoPlayer -> VideoViewerDialogCard(
            uri = dialog.uri,
            title = dialog.title,
            onDismiss = threadViewModel::dismissDialog
        )

        ErrorAttachmentNotSupported -> ErrorDialog(
            title = stringResource(id = R.string.attachment_upload_failure),
            message = stringResource(id = R.string.attachment_not_supported),
            onDismiss = threadViewModel::dismissDialog
        )

        ErrorAttachmentTooLarge -> ErrorDialog(
            title = stringResource(id = R.string.attachment_upload_failure),
            message = stringResource(id = R.string.attachment_too_large, threadViewModel.maxAttachmentSize),
            onDismiss = threadViewModel::dismissDialog
        )

        ErrorUnableToReadAttachment -> ErrorDialog(
            title = stringResource(id = R.string.attachment_upload_failure),
            message = stringResource(id = R.string.attachment_read_error),
            onDismiss = threadViewModel::dismissDialog
        )

        EndContact -> EndContactDialog(closeChat = closeChat, chatViewModel = threadViewModel, chatModel = chatModel)
    }

    if (threadViewModel.preparingToShare.collectAsState().value) {
        BusySpinner(message = stringResource(R.string.preparing))
    }
}
