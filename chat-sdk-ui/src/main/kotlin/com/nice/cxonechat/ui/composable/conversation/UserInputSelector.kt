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

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.InputState.Attachment
import com.nice.cxonechat.ui.composable.conversation.InputState.Audio
import com.nice.cxonechat.ui.composable.conversation.InputState.None
import com.nice.cxonechat.ui.composable.theme.ChatIconButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.SendButton
import com.nice.cxonechat.ui.data.RequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Composable for the user input selector row in the chat UI.
 *
 * Displays the input field, attachment picker, send button, and audio recording controls.
 * Handles transitions between different input states (text, attachment, audio).
 *
 * @param onSelectorChange Callback to change the current input state.
 * @param sendMessageEnabled State controlling whether the send button is enabled.
 * @param showSendButton State controlling visibility of the send button.
 * @param onMessageSent Callback invoked when a message is sent.
 * @param audioRecordingUiState State and actions for audio recording UI.
 * @param currentInputSelector The current input state (None, Attachment, Audio).
 * @param focusRequester FocusRequester for managing input focus.
 * @param showMessageProcessing Whether to show a loading indicator for message processing.
 * @param modifier Modifier for styling the row.
 * @param onError Callback for error messages.
 * @param content Composable content for the input field (usually a text field).
 */
@Composable
internal fun UserInputSelector(
    onSelectorChange: (InputState) -> Unit,
    sendMessageEnabled: State<Boolean>,
    showSendButton: State<Boolean>,
    onMessageSent: () -> Unit,
    audioRecordingUiState: AudioRecordingUiState,
    currentInputSelector: InputState,
    focusRequester: FocusRequester,
    showMessageProcessing: Boolean,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val rowMod = modifier
        .height(72.dp)
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        .focusGroup()
    val onAudioRecordToggle = audioRecordingUiState.onAudioRecordToggle
    val coroutineScope = rememberCoroutineScope()
    val loading = stringResource(string.loading)
    AnimatedContent(currentInputSelector) { state ->
        when (state) {
            None, Attachment -> Row(
                modifier = rowMod,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val configuration = koinInject<Configuration?>()
                val showAddAttachments: Boolean = remember(configuration) {
                    configuration?.fileRestrictions?.isAttachmentsEnabled ?: false
                }
                if (showAddAttachments) {
                    ChatIconButton(
                        icon = Outlined.Add,
                        description = stringResource(string.title_attachment_picker)
                    ) {
                        focusRequester.freeFocus()
                        onSelectorChange(Attachment)
                    }
                }
                content()
                AnimatedContent(showSendButton.value) { sendMessage ->
                    when {
                        showMessageProcessing ->
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .semantics {
                                        testTag = "attachment_upload_spinner"
                                        contentDescription = loading
                                    }
                            )

                        sendMessage -> SendButton(enabled = sendMessageEnabled.value, onMessageSent = onMessageSent)
                        else -> AudioRecorderButton(onAudioRecordToggle, onSelectorChange, onError, coroutineScope)
                    }
                }
            }

            Audio -> AudioInputRow(audioRecordingUiState, coroutineScope, rowMod) { onSelectorChange(None) }
        }
    }
}

internal enum class InputState {
    None,
    Attachment,
    Audio,
}

@Composable
private fun AudioRecorderButton(
    onAudioRecordToggle: suspend () -> RequestResult,
    onSelectorChange: (InputState) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val resources = LocalResources.current
    ChatIconButton(
        icon = Icons.Default.Mic,
        description = stringResource(string.record_audio_start_content_description),
        onClick = remember(resources, scope) {
            {
                scope.launch {
                    val toggleChangeResult = onAudioRecordToggle()
                    if (toggleChangeResult == RequestResult.SUCCESS) {
                        onSelectorChange(Audio)
                    } else {
                        if (toggleChangeResult == RequestResult.FAILURE) {
                            onError(resources.getString(string.recording_audio_failed_to_start))
                        }
                        onSelectorChange(None) // Failed to start audio recording
                    }
                }
            }
        }
    )
}

// region Previews

/**
 * Interactive preview of UserInputSelector that allows switching between different input states.
 * In interactive mode, you can:
 * - Toggle between "None" and "Attachment" states using the + button
 * - Toggle send button visibility
 * - Toggle send button enabled state
 * - Toggle message processing state
 * - Switch to Audio recording state using the mic button
 */
@Suppress(
    "LongMethod", // Preview method for interactive testing
)
@Preview(name = "User Input Selector - Interactive", showBackground = true)
@Composable
private fun UserInputSelectorPreview() {
    var currentInputState by remember { mutableStateOf(None) }
    var inputState by remember { mutableIntStateOf(0) }
    val showSendButton = remember { mutableStateOf(false) }
    val sendMessageEnabled = remember { mutableStateOf(true) }
    var showMessageProcessing by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ChatTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                // Preview controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text("Preview Controls", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = {
                        inputState = (inputState + 1).mod(InputState.entries.size)
                        currentInputState = InputState.entries[inputState]
                    }) {
                        Text("Current State: ${currentInputState.name}")
                    }
                    Button(onClick = { showSendButton.value = !showSendButton.value }) {
                        Text("Show Send Button: ${showSendButton.value}")
                    }
                    Button(onClick = { sendMessageEnabled.value = !sendMessageEnabled.value }) {
                        Text("Send Enabled: ${sendMessageEnabled.value}")
                    }
                    Button(onClick = { showMessageProcessing = !showMessageProcessing }) {
                        Text("Processing: $showMessageProcessing")
                    }
                    errorMessage?.let { error ->
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Actual component
                UserInputSelector(
                    onSelectorChange = { currentInputState = it },
                    sendMessageEnabled = sendMessageEnabled,
                    showSendButton = showSendButton,
                    onMessageSent = { errorMessage = "Message sent!" },
                    audioRecordingUiState = createPreviewState(),
                    currentInputSelector = currentInputState,
                    focusRequester = remember { FocusRequester() },
                    showMessageProcessing = showMessageProcessing,
                    onError = { errorMessage = it }
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            showSendButton.value = it.isNotEmpty()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("Type a message...") }
                    )
                }
            }
        }
    }
}

/**
 * Preview showing the None/default state with audio recording button.
 */
@Preview(name = "Input State: None (with Mic button)", showBackground = true)
@Composable
private fun UserInputSelectorNoneStatePreview() {
    ChatTheme {
        Surface {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createPreviewState(),
                currentInputSelector = None,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Type a message...") }
                )
            }
        }
    }
}

/**
 * Preview showing the state with send button enabled.
 */
@Preview(name = "Input State: None (with Send button)", showBackground = true)
@Composable
private fun UserInputSelectorWithSendButtonPreview() {
    ChatTheme {
        Surface {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(true) },
                onMessageSent = {},
                audioRecordingUiState = createPreviewState(),
                currentInputSelector = None,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "Sample message text",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Type a message...") }
                )
            }
        }
    }
}

/**
 * Preview showing the message processing state with spinner.
 */
@Preview(name = "Input State: Processing (uploading attachment)", showBackground = true)
@Composable
private fun UserInputSelectorProcessingPreview() {
    ChatTheme {
        Surface {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(false) },
                showSendButton = remember { mutableStateOf(true) },
                onMessageSent = {},
                audioRecordingUiState = createPreviewState(),
                currentInputSelector = None,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = true,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "Message with attachment",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Type a message...") }
                )
            }
        }
    }
}

/**
 * Preview showing the Audio recording state.
 */
@Preview(name = "Input State: Audio Recording", showBackground = true)
@Composable
private fun UserInputSelectorAudioStatePreview() {
    ChatTheme {
        Surface {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createPreviewState(isRecording = true),
                currentInputSelector = Audio,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Type a message...") }
                )
            }
        }
    }
}

/**
 * Preview showing the send button in disabled state.
 */
@Preview(name = "Input State: Send Button Disabled", showBackground = true)
@Composable
private fun UserInputSelectorSendDisabledPreview() {
    ChatTheme {
        Surface {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(false) },
                showSendButton = remember { mutableStateOf(true) },
                onMessageSent = {},
                audioRecordingUiState = createPreviewState(),
                currentInputSelector = None,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "Message text",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Type a message...") }
                )
            }
        }
    }
}

/**
 * Helper function to create a mock AudioRecordingUiState for previews.
 */
private fun createPreviewState(
    isRecording: Boolean = false,
    duration: Duration = 0.seconds,
): AudioRecordingUiState = AudioRecordingUiState(
    isRecordingAllowedFlow = flowOf(true),
    uriFlow = MutableStateFlow(Uri.EMPTY),
    isRecordingFlow = MutableStateFlow(isRecording),
    durationFlow = MutableStateFlow(duration),
    onDismiss = {},
    onApprove = {},
    onAudioRecordToggle = { RequestResult.SUCCESS }
)

// endregion
