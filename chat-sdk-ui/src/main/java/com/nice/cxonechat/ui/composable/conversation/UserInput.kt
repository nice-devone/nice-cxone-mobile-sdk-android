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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.InputState.Attachment
import com.nice.cxonechat.ui.composable.conversation.InputState.Audio
import com.nice.cxonechat.ui.composable.conversation.InputState.None
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.generic.AttachmentPickerDialog
import com.nice.cxonechat.ui.composable.generic.toastAudioRecordToggleFailure
import com.nice.cxonechat.ui.composable.theme.ChatIconButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SendButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
@Composable
internal fun UserInput(
    conversationUiState: ConversationUiState,
    audioRecordingUiState: AudioRecordingUiState,
    onAttachmentTypeSelection: (attachmentType: AttachmentType) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
) = UserInput(
    conversationUiState = conversationUiState,
    audioRecordingUiState = audioRecordingUiState,
    onAttachmentTypeSelection = onAttachmentTypeSelection,
    modifier = modifier,
    resetScroll = resetScroll,
    initialInputSelector = None,
)

@Suppress(
    "CognitiveComplexMethod",
    "LongMethod"
)
@Composable
private fun UserInput(
    conversationUiState: ConversationUiState,
    audioRecordingUiState: AudioRecordingUiState,
    onAttachmentTypeSelection: (attachmentType: AttachmentType) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
    initialInputSelector: InputState = None,
) {
    var currentInputSelector by rememberSaveable { mutableStateOf(initialInputSelector) }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    val dismissKeyboard = {
        currentInputSelector = None
        textFieldFocusState = false
    }

    // Intercept back navigation if there's an InputSelector visible
    if (currentInputSelector != None) {
        BackHandler(onBack = dismissKeyboard)
    }

    val textState by rememberSaveable(stateSaver = TextFieldState.Saver) {
        mutableStateOf(TextFieldState())
    }

    var isTypingState by remember { mutableStateOf(false) }

    val signalStoppedTyping = {
        if (isTypingState) {
            isTypingState = false
            conversationUiState.onStopTyping()
        }
    }
    val attachments = conversationUiState.pendingAttachments.collectAsState()
    val sendMessage = {
        // trim surrounding spaces
        val text = textState.text.toString().trim()

        // Don't send blank messages.
        if (text.isNotEmpty() || attachments.value.isNotEmpty()) {
            conversationUiState.sendMessage(OutboundMessage(text))
            // Reset text field and close keyboard
            textState.clearText()
            // Move scroll to bottom
            resetScroll()
            dismissKeyboard()
            if (isTypingState) signalStoppedTyping()
        }
    }

    // Observe text changes and signal typing state
    LaunchedEffect(Unit) {
        snapshotFlow { textState.text }.collectLatest { newText ->
            if (newText.isNotEmpty()) {
                if (!isTypingState) {
                    isTypingState = true
                    conversationUiState.onStartTyping()
                }
            } else {
                signalStoppedTyping()
            }
        }
    }

    Surface(
        modifier = Modifier
            .testTag("user_input")
            .then(modifier),
        color = colorScheme.background,
    ) {
        Column {
            AttachmentPreviewBar(
                attachments = attachments.value,
                onAttachmentClick = conversationUiState.onAttachmentClicked,
                onAttachmentRemoved = conversationUiState.onRemovePendingAttachment
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = space.large))
            val sendMessageEnabledState = remember {
                derivedStateOf {
                    textState.text.isNotBlank() || attachments.value.isNotEmpty()
                }
            }
            val isRecordingAllowed by audioRecordingUiState.isRecordingAllowedFlow.collectAsState(true)
            val showSendButton = remember {
                derivedStateOf {
                    !isRecordingAllowed || sendMessageEnabledState.value
                }
            }
            Row {
                UserInputSelector(
                    onSelectorChange = { currentInputSelector = it },
                    sendMessageEnabled = sendMessageEnabledState,
                    showSendButton = showSendButton,
                    onMessageSent = sendMessage,
                    audioRecordingUiState = audioRecordingUiState,
                    currentInputSelector = currentInputSelector
                ) {
                    UserInputText(
                        textFieldValue = textState,
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
                    )
                }
                SelectorExpanded(
                    onAttachmentTypeSelection = onAttachmentTypeSelection,
                    onCloseRequested = dismissKeyboard,
                    currentSelector = currentInputSelector,
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
private fun RowScope.UserInputText(
    keyboardType: KeyboardType = KeyboardType.Text,
    textFieldValue: TextFieldState,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    onSend: () -> Unit,
    focusState: Boolean,
) {
    val a11yLabel = stringResource(string.content_description_text_input)
    Box(
        modifier = Modifier
            .height(64.dp)
            .weight(1f)
            .semantics {
                contentDescription = a11yLabel
                keyboardShownProperty = keyboardShown
                testTag = "user_input_text"
            },
    ) {
        var lastFocusState by remember { mutableStateOf(false) }
        BasicTextField(
            state = textFieldValue,
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
            onKeyboardAction = {
                onSend()
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalContentColor.current),
            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
        )

        val disableContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
        if (textFieldValue.text.isEmpty() && !focusState) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 16.dp),
                text = stringResource(string.hint_enter_a_message),
                style = ChatTheme.typography.bodyLarge.copy(color = disableContentColor)
            )
        }
    }
}

@Composable
private fun UserInputSelector(
    onSelectorChange: (InputState) -> Unit,
    sendMessageEnabled: State<Boolean>,
    showSendButton: State<Boolean>,
    onMessageSent: () -> Unit,
    audioRecordingUiState: AudioRecordingUiState,
    currentInputSelector: InputState,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val rowMod = modifier
        .height(72.dp)
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    val onAudioRecordToggle = audioRecordingUiState.onAudioRecordToggle
    val coroutineScope = rememberCoroutineScope()
    AnimatedContent(currentInputSelector) { state ->
        when (state) {
            None, Attachment -> Row(
                modifier = rowMod,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatIconButton(
                    icon = Outlined.Add,
                    description = stringResource(string.title_attachment_picker)
                ) { onSelectorChange(Attachment) }
                content()
                AnimatedContent(showSendButton.value) { sendMessage ->
                    when (sendMessage) {
                        true -> SendButton(enabled = sendMessageEnabled.value, onMessageSent = onMessageSent)
                        false -> AudioRecorderButton(onAudioRecordToggle, onSelectorChange, coroutineScope)
                    }
                }
            }

            Audio -> AudioInputRow(audioRecordingUiState, coroutineScope, rowMod) { onSelectorChange(None) }
        }
    }
}

@Composable
private fun AudioRecorderButton(
    onAudioRecordToggle: suspend () -> Boolean,
    onSelectorChange: (InputState) -> Unit,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    ChatIconButton(
        icon = Icons.Default.Mic,
        description = stringResource(string.record_audio_start_content_description),
        onClick = remember(context, scope) {
            {
                scope.launch {
                    val toggleChangeResult = onAudioRecordToggle()
                    if (toggleChangeResult) {
                        onSelectorChange(Audio)
                    } else {
                        context.toastAudioRecordToggleFailure(false)
                        onSelectorChange(None) // Failed to start audio recording
                    }
                }
            }
        }
    )
}

@Composable
private fun SelectorExpanded(
    onAttachmentTypeSelection: (attachmentType: AttachmentType) -> Unit,
    currentSelector: InputState,
    onCloseRequested: () -> Unit,
) {
    if (currentSelector === None || currentSelector === Audio) return

    Surface {
        when (currentSelector) {
            Attachment -> AttachmentPickerDialog(onCloseRequested, onAttachmentTypeSelection)
            else -> throw NotImplementedError()
        }
    }
}

internal enum class InputState {
    None,
    Attachment,
    Audio,
}

@PreviewLightDark
@Composable
private fun UserInputPreview() {
    ChatTheme {
        Surface {
            UserInput(
                conversationUiState = previewUiState(pendingAttachments = PreviewAttachments.choices),
                audioRecordingUiState = previewAudioState(),
                onAttachmentTypeSelection = {},
            )
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun UserInputAudioPreview() {
    ChatTheme {
        Surface {
            UserInput(
                conversationUiState = previewUiState(),
                audioRecordingUiState = previewAudioState(),
                onAttachmentTypeSelection = {},
                initialInputSelector = Audio,
            )
        }
    }
}
