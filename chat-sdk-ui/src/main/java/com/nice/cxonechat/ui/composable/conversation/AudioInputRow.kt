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

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.nice.cxonechat.ui.composable.player.PlaceholderState
import com.nice.cxonechat.ui.composable.player.PlayerState
import com.nice.cxonechat.ui.composable.player.produceAudioPlayerState
import com.nice.cxonechat.ui.composable.player.rememberPlayerState
import com.nice.cxonechat.ui.composable.theme.ChatIconButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SendButton
import com.nice.cxonechat.ui.composable.theme.SmallSpacer
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.ErrorGroup.LOW
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.util.toTimeStamp
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
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
        SmallSpacer()
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
        RecordTextInfo(isAudioRecording, audioPlayerState, audioRecordingUiState.durationFlow)
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
    Column(
        modifier = Modifier
            .padding(horizontal = space.medium)
            .widthIn(min = 224.dp)
            .weight(1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(space.audioRecordingTextPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(space.clickableSize - 1.dp) // -1dp to compensate divider height
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(space.medium))
                Text(
                    text = audioText,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = chatTypography.audioRecordingLabel,
                )
            }
            Text(
                text = audioTime.toTimeStamp(Locale.current),
                modifier = Modifier.alpha(0.5f),
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                style = chatTypography.audioRecordingTime,
                color = chatColors.token.content.tertiary
            )
        }
        HorizontalDivider(color = chatColors.token.border.default)
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
    val chatStateViewModel =
        if (!LocalInspectionMode.current && LocalActivity.current is ChatActivity) {
            koinActivityViewModel<ChatStateViewModel>()
        } else {
            null
        }
    val stopRecording: () -> Unit = remember(scope, context) {
        {
            scope.launch {
                val toggleChangeResult = onAudioRecordToggle()
                if (!toggleChangeResult) {
                    chatStateViewModel?.showError(LOW, context.getString(R.string.recording_audio_failed))
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
        Surface(
            modifier = Modifier.systemBarsPadding(),
            color = colorScheme.background
        ) {
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
        Surface(
            modifier = Modifier.systemBarsPadding(),
            color = colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                AudioInputRow(previewAudioState(), rememberCoroutineScope()) { }
            }
        }
    }
}
