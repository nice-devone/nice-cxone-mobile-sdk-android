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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.generic.AudioPlayerButton
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.generic.toastAudioRecordToggleFailure
import com.nice.cxonechat.ui.composable.player.PlaceholderState
import com.nice.cxonechat.ui.composable.player.PlayerState
import com.nice.cxonechat.ui.composable.player.produceAudioPlayerState
import com.nice.cxonechat.ui.composable.player.rememberPlayerState
import com.nice.cxonechat.ui.composable.theme.ChatIconButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.SendButton
import com.nice.cxonechat.ui.composable.theme.SmallSpacer
import com.nice.cxonechat.ui.composable.theme.TinySpacer
import com.nice.cxonechat.ui.util.toTimeStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Row of User input buttons which allow the user to stop the audio recording, review the audio record,
 * and send or discard the audio.
 * It also provides UI informing user about audio recording/playback state.
 *
 * @param audioRecordingUiState State of the audio recording and means how to trigger associated actions.
 * @param coroutineScope CoroutineScope used to stop the audio recording.
 * @param modifier Modifier applied to the [Row] containing UI elements.
 * @param onSelectorChange Callback which should be triggered when the UI should trigger to different input state.
 */
@Composable
internal fun AudioInputRow(
    audioRecordingUiState: AudioRecordingUiState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    onSelectorChange: () -> Unit,
) {
    val isAudioRecording by audioRecordingUiState.isRecordingFlow.collectAsState()
    val audioUri = audioRecordingUiState.uriFlow
    val onAudioRecordToggle = audioRecordingUiState.onAudioRecordToggle
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DeleteButton {
            audioRecordingUiState.onDismiss
            onSelectorChange()
        }
        TinySpacer()
        val recordingUri by audioUri.collectAsState()
        val context = LocalContext.current
        val playerResult by produceAudioPlayerState(context, recordingUri)
        val audioPlayerState = when (playerResult) {
            Loading -> PlaceholderState
            is Ready -> rememberPlayerState((playerResult as Ready).player)
        }
        AnimatedContent(isAudioRecording) { recording ->
            when (recording) {
                true -> StopRecordingButton(coroutineScope, onAudioRecordToggle, onSelectorChange)
                false -> AudioPlayerButton(playerResult)
            }
        }
        SmallSpacer()
        RecordTextInfo(isAudioRecording, audioPlayerState, audioRecordingUiState.durationFlow)
        SmallSpacer()
        SendButton(
            enabled = !isAudioRecording && playerResult is Ready, // The player will be ready only for valid URIs
            onMessageSent = {
                audioRecordingUiState.onApprove(recordingUri)
                onSelectorChange()
            }
        )
    }
}

@Composable
private fun RowScope.RecordTextInfo(
    isAudioRecording: Boolean,
    audioPlayerState: PlayerState,
    durationStateFlow: StateFlow<Duration>,
) {
    val audioText = if (isAudioRecording) {
        stringResource(R.string.recording_audio_hint)
    } else {
        stringResource(R.string.recording_audio_preview_label)
    }
    val audioTime by if (isAudioRecording) {
        durationStateFlow.collectAsState()
    } else if (audioPlayerState.isPlaying.value) {
        audioPlayerState.position
    } else {
        audioPlayerState.duration
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .widthIn(min = 224.dp)
            .weight(1f)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Text(
                text = audioText,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ChatTheme.chatTypography.audioRecordingLabel,
            )
        }
        Text(
            text = audioTime.toTimeStamp(Locale.current),
            modifier = Modifier.alpha(0.5f),
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            style = ChatTheme.chatTypography.audioRecordingTime,
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    ChatIconButton(
        icon = Icons.Default.Delete,
        description = stringResource(R.string.record_audio_delete_content_description),
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = colorScheme.error,
            contentColor = IconButtonDefaults.filledIconButtonColors().contentColor
        )
    )
}

@Composable
private fun StopRecordingButton(
    scope: CoroutineScope,
    onAudioRecordToggle: suspend () -> Boolean,
    onSelectorChange: () -> Unit,
) {
    val context = LocalContext.current
    val stopRecording: () -> Unit = remember(scope, context) {
        {
            scope.launch {
                val toggleChangeResult = onAudioRecordToggle()
                if (!toggleChangeResult) {
                    context.toastAudioRecordToggleFailure(true)
                    onSelectorChange()
                }
            }
        }
    }
    ChatIconButton(
        icon = Icons.Default.Stop,
        description = stringResource(R.string.record_audio_stop_content_description),
        onClick = stopRecording
    )
}

@PreviewLightDark
@Composable
private fun PreviewButtons() {
    ChatTheme {
        Surface {
            Column(Modifier.width(Min)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Text("Delete")
                    DeleteButton {}
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Text("Stop")
                    StopRecordingButton(rememberCoroutineScope(), { true }, {})
                }
            }
        }
    }
}

@PreviewFontScale
@Composable
private fun PreviewAudioText() {
    ChatTheme {
        Surface {
            Column(Modifier.width(Min)) {
                val duration = remember { MutableStateFlow(Duration.ZERO) }
                Row {
                    RecordTextInfo(true, PlaceholderState, duration)
                }
                Row {
                    RecordTextInfo(false, PlaceholderState, duration)
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PreviewRow() {
    ChatTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AudioInputRow(previewAudioState(), rememberCoroutineScope()) { }
            }
        }
    }
}
