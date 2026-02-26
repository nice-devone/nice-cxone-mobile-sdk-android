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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.ErrorAlertDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.SendTranscriptDialog
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
internal fun SendTranscriptDialogView(
    threadViewModel: ChatThreadViewModel,
) {
    val onDismiss = remember { threadViewModel::dismissSendTranscriptDialog }
    val showFullScreenLoading by threadViewModel.showFullScreenLoading.collectAsState()

    when (val dialog = threadViewModel.showTranscriptDialog.collectAsState(SendTranscriptDialog.None).value) {
        SendTranscriptDialog.None -> Unit
        SendTranscriptDialog.SendTranscript -> SendTranscriptBottomSheet(
            onDismiss = onDismiss,
            onSubmit = { threadViewModel.sendTranscript(it) },
            showFullScreenLoading = showFullScreenLoading
        )

        is SendTranscriptDialog.SendTranscriptUpdateDialog -> SendTranscriptUpdateWrapper(dialog, onDismiss)
    }
}

@Composable
private fun SendTranscriptUpdateWrapper(
    dialog: SendTranscriptDialog.SendTranscriptUpdateDialog,
    onDismiss: () -> Unit,
) {
    SendTranscriptUpdateDialog(
        title = stringResource(if (dialog.isSuccess) string.send_transcript else string.error),
        message = stringResource(
            if (dialog.isSuccess) string.send_transcript_success_message else string.send_transcript_failed_message
        ),
        onDismiss = onDismiss
    )
}

@Composable
private fun SendTranscriptUpdateDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    ChatTheme.ErrorAlertDialog(
        title = title,
        body = message,
        modifier = Modifier,
        buttonText = stringResource(string.close),
        onConfirmClick = onDismiss
    )
}
