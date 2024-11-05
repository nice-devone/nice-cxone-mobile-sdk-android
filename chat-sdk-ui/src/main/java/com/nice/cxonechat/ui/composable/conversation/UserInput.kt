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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.InputSelector.Attachment
import com.nice.cxonechat.ui.composable.conversation.InputSelector.Audio
import com.nice.cxonechat.ui.composable.conversation.InputSelector.None
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.generic.AttachmentPickerDialog
import com.nice.cxonechat.ui.composable.generic.AudioRecordingDialog
import com.nice.cxonechat.ui.composable.generic.toastAudioRecordToggleFailure
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SelectableIconButton
import com.nice.cxonechat.ui.composable.theme.SmallSpacer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class InputSelector {
    None,
    Attachment,
    Audio
}

/**
 * Provides UI for various user inputs which is presented to user on a [Surface] as a base.
 *
 * Supports text and attachments (TBD).
 * Text input automatically expands keyboard and when keyboard is closed the [resetScroll] is invoked.
 *
 * @param conversationUiState Holder for UI state of conversation.
 * @param audioRecordingUiState Holder for UI state of audio recording.
 * @param onAttachmentTypeSelection Action called when the user has selected an attachment type,
 * which should be retrieved and sent as an attachment message.
 * @param modifier Modifier for visible content. It should supply paddings for navigation and ime keyboard.
 * @param resetScroll Action invoked when the user closes the keyboard for the text input.
 */
@Suppress(
    "CognitiveComplexMethod",
    "LongMethod"
)
@Composable
internal fun UserInput(
    conversationUiState: ConversationUiState,
    audioRecordingUiState: AudioRecordingUiState,
    onAttachmentTypeSelection: (mimeType: Collection<String>) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
) {
    val isAudioRecording = audioRecordingUiState.isRecordingFlow.collectAsState().value
    var currentInputSelector by rememberSaveable { mutableStateOf(None) }
    val dismissKeyboard = { currentInputSelector = None }

    // Intercept back navigation if there's an InputSelector visible
    if (currentInputSelector != None) {
        BackHandler(onBack = dismissKeyboard)
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    var isTypingState by remember { mutableStateOf(false) }

    val signalStoppedTyping = {
        if (isTypingState) {
            isTypingState = false
            conversationUiState.onStopTyping()
        }
    }

    val sendMessage = {
        // trim surrounding spaces
        val text = textState.text.trim()

        // Don't send blank messages.
        if (text.isNotEmpty()) {
            conversationUiState.sendMessage(OutboundMessage(text))
            // Reset text field and close keyboard
            textState = TextFieldValue()
            // Move scroll to bottom
            resetScroll()
            dismissKeyboard()
            if (isTypingState) signalStoppedTyping()
        }
    }

    Surface(modifier = modifier) {
        Column {
            Header()
            Row {
                UserInputSelector(
                    onSelectorChange = { currentInputSelector = it },
                    sendMessageEnabled = textState.text.isNotBlank(),
                    onMessageSent = sendMessage,
                    currentInputSelector = currentInputSelector,
                    isAudioRecording = isAudioRecording,
                    onAudioRecordToggle = audioRecordingUiState.onAudioRecordToggle,
                ) {
                    UserInputText(
                        textFieldValue = textState,
                        onTextChanged = {
                            textState = it
                            if (it.text.isNotEmpty()) {
                                if (!isTypingState) {
                                    isTypingState = true
                                    conversationUiState.onStartTyping()
                                }
                            } else {
                                signalStoppedTyping()
                            }
                        },
                        // Only show the keyboard if there's no input selector and text field has focus
                        keyboardShown = currentInputSelector == None && textFieldFocusState,
                        // Close extended selector if text field receives focus
                        onTextFieldFocused = { focused ->
                            if (focused) {
                                currentInputSelector = None
                                resetScroll()
                            } else {
                                signalStoppedTyping()
                            }
                            textFieldFocusState = focused
                        },
                        onSend = sendMessage,
                        focusState = textFieldFocusState,
                        isAudioRecording = isAudioRecording
                    )
                }
                SelectorExpanded(
                    audioRecordingUiState = audioRecordingUiState,
                    onAttachmentTypeSelection = onAttachmentTypeSelection,
                    onCloseRequested = dismissKeyboard,
                    currentSelector = currentInputSelector,
                    onSelectorChange = { currentInputSelector = it },
                )
            }
        }
    }

    LaunchedEffect(key1 = textState, key2 = textFieldFocusState, key3 = isTypingState) {
        delay(1500L) // Delay to prevent excessive signaling
        signalStoppedTyping()
    }
    DisposableEffect(true) {
        onDispose {
            signalStoppedTyping()
        }
    }
}

private val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
private var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .height(24.dp)
            .padding(vertical = space.medium, horizontal = space.large)
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun RowScope.UserInputText(
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    onSend: () -> Unit,
    focusState: Boolean,
    isAudioRecording: Boolean,
) {
    val a11yLabel = stringResource(string.content_description_text_input)
    Box(
        modifier = Modifier
            .height(64.dp)
            .weight(1f)
            .semantics {
                contentDescription = a11yLabel
                keyboardShownProperty = keyboardShown
            },
    ) {
        var lastFocusState by remember { mutableStateOf(false) }
        BasicTextField(
            value = textFieldValue,
            onValueChange = { onTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
                .align(Alignment.CenterStart)
                .onFocusChanged { state ->
                    if (lastFocusState != state.isFocused) {
                        onTextFieldFocused(state.isFocused)
                    }
                    lastFocusState = state.isFocused
                }
                .semantics {
                    testTag = "chat_text_field"
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSend() }
            ),
            maxLines = 1,
            cursorBrush = SolidColor(LocalContentColor.current),
            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
        )

        val disableContentColor = ChatTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        if (textFieldValue.text.isEmpty() && !focusState) {
            val hint = if (isAudioRecording) {
                stringResource(string.recording_audio_hint)
            } else {
                stringResource(string.hint_enter_a_message)
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 16.dp),
                text = hint,
                style = ChatTheme.typography.bodyLarge.copy(color = disableContentColor)
            )
        }
    }
}

@Composable
private fun UserInputSelector(
    onSelectorChange: (InputSelector) -> Unit,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    isAudioRecording: Boolean,
    onAudioRecordToggle: suspend () -> Boolean,
    currentInputSelector: InputSelector,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(72.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatTheme.SelectableIconButton(
            icon = Outlined.Add,
            description = stringResource(string.title_attachment_picker),
            selected = currentInputSelector == Attachment
        ) { onSelectorChange(Attachment) }
        SmallSpacer()
        AudioRecorderButton(isAudioRecording, onAudioRecordToggle, onSelectorChange)
        val border = if (!sendMessageEnabled) {
            BorderStroke(
                width = 1.dp,
                color = ChatTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        } else {
            null
        }

        content()

        SendButton(sendMessageEnabled, onMessageSent, border)
    }
}

@Composable
private fun AudioRecorderButton(
    isAudioRecording: Boolean,
    onAudioRecordToggle: suspend () -> Boolean,
    onSelectorChange: (InputSelector) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ChatTheme.InputSelectorToggleButton(
        icon = if (isAudioRecording) Outlined.Mic else Outlined.MicNone,
        description = stringResource(string.title_attachment_picker),
        checked = isAudioRecording,
    ) { toggle ->
        scope.launch {
            val toggleChangeResult = onAudioRecordToggle()
            if (!toggleChangeResult) context.toastAudioRecordToggleFailure(isAudioRecording)
            if (!toggle && isAudioRecording && toggleChangeResult) {
                onSelectorChange(Audio)
            } else {
                onSelectorChange(None) // Failed to start audio recording
            }
        }
    }
}

@Composable
private fun SendButton(
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    border: BorderStroke?,
) {
    OutlinedButton(
        modifier = Modifier
            .height(Max)
            .width(Min),
        enabled = sendMessageEnabled,
        onClick = onMessageSent,
        border = border,
        contentPadding = PaddingValues(8.dp)
    ) {
        Icon(
            imageVector = AutoMirrored.Outlined.Send,
            contentDescription = stringResource(string.text_send)
        )
    }
}

@Composable
private fun ChatTheme.InputSelectorToggleButton(
    icon: ImageVector,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val selectedColor = if (checked) colorScheme.primary else colorScheme.secondary
    val backgroundModifier = Modifier.background(
        color = selectedColor,
        shape = RoundedCornerShape(8.dp)
    )
    IconToggleButton(
        checked = checked,
        onCheckedChange = onToggle,
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = colorScheme.secondary,
            contentColor = colorScheme.onSecondary,
            checkedContainerColor = colorScheme.primary,
            checkedContentColor = colorScheme.onPrimary,
        ),
        modifier = Modifier.then(backgroundModifier)
    ) {
        Icon(
            icon,
            modifier = Modifier.padding(4.dp),
            contentDescription = description
        )
    }
}

@Composable
private fun SelectorExpanded(
    audioRecordingUiState: AudioRecordingUiState,
    onAttachmentTypeSelection: (mimeType: Collection<String>) -> Unit,
    currentSelector: InputSelector,
    onCloseRequested: () -> Unit,
    onSelectorChange: (InputSelector) -> Unit,
) {
    if (currentSelector == None) return

    Surface(shadowElevation = 8.dp, tonalElevation = 8.dp) {
        when (currentSelector) {
            Attachment -> AttachmentPickerDialog(onCloseRequested, onAttachmentTypeSelection)
            Audio -> AudioRecordingDialog(
                audioRecordingUiState = audioRecordingUiState.copy(onDismiss = {
                    onCloseRequested()
                    audioRecordingUiState.onDismiss()
                    onSelectorChange(None)
                })
            )

            else -> throw NotImplementedError()
        }
    }
}

@Preview
@Composable
private fun UserInputPreview() {
    ChatTheme {
        UserInput(
            conversationUiState = previewUiState(),
            onAttachmentTypeSelection = {},
            audioRecordingUiState = previewAudioState(),
        )
    }
}
