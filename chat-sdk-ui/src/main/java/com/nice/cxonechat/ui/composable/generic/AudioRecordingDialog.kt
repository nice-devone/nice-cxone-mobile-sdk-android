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

package com.nice.cxonechat.ui.composable.generic

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.OutlinedButton
import com.nice.cxonechat.ui.composable.theme.SmallSpacer
import kotlinx.coroutines.launch

@Composable
internal fun AudioRecordingDialog(
    audioRecordingUiState: AudioRecordingUiState,
) {
    val uri = audioRecordingUiState.uriFlow.collectAsState().value
    val openDialog = remember { mutableStateOf(true) }
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                audioRecordingUiState.onDismiss()
                openDialog.value = false
            },
            confirmButton = {
                ConfirmButton(uri) {
                    audioRecordingUiState.onApprove(it)
                    openDialog.value = false
                }
            },
            dismissButton = {
                ChatTheme.OutlinedButton(stringResource(id = string.cancel)) {
                    audioRecordingUiState.onDismiss()
                    openDialog.value = false
                }
            },
            title = { DialogTitle(stringResource(string.recording_audio_preview_title)) },
            text = { DialogContent(audioRecordingUiState) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DialogContent(
    audioRecordingUiState: AudioRecordingUiState,
) {
    Column {
        Text(stringResource(string.recording_audio_preview_message))
        Spacer(modifier = Modifier.size(space.large))
        AudioPlayer(audioRecordingUiState.uriFlow.collectAsState().value)
        ToggleRecordButton(
            isRecording = audioRecordingUiState.isRecordingFlow.collectAsState().value,
            onAudioRecordToggle = audioRecordingUiState.onAudioRecordToggle,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun ConfirmButton(uri: Uri, onApprove: (Uri) -> Unit) {
    OutlinedButton(onClick = { onApprove(uri) }) {
        Row {
            Icon(AutoMirrored.Outlined.Send, stringResource(string.send_audio_message_content_description))
            SmallSpacer()
            Text(stringResource(string.text_send))
        }
    }
}

@Composable
private fun ToggleRecordButton(
    isRecording: Boolean,
    onAudioRecordToggle: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            scope.launch {
                if (!onAudioRecordToggle()) context.toastAudioRecordToggleFailure(isRecording)
            }
        },
        contentPadding = PaddingValues(space.small),
        modifier = modifier.defaultMinSize(space.clickableSize, space.clickableSize)
    ) {
        if (isRecording) {
            Icon(Outlined.Stop, contentDescription = stringResource(string.record_audio_stop_content_description))
        } else {
            Icon(Outlined.Refresh, contentDescription = stringResource(string.record_audio_retry_content_description))
        }
    }
}

@Preview
@Composable
private fun PreviewDialog() {
    ChatTheme {
        AudioRecordingDialog(previewAudioState())
    }
}
