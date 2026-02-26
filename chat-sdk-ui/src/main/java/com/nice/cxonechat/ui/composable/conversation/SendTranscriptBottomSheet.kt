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

import android.util.Patterns.EMAIL_ADDRESS
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.Requirement
import com.nice.cxonechat.ui.composable.generic.Requirements.allOf
import com.nice.cxonechat.ui.composable.generic.Requirements.email
import com.nice.cxonechat.ui.composable.generic.Requirements.required
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TextField
import com.nice.cxonechat.ui.screen.LoadingOverlayFullScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SendTranscriptBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    showFullScreenLoading: Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        it === Expanded
    }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
            }
        },
        contentWindowInsets = { WindowInsets() },
        sheetState = sheetState,
        containerColor = chatColors.token.background.default,
        contentColor = chatColors.token.content.primary,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(chatColors.token.background.surface.subtle)
            ) {
                Surface(
                    modifier =
                        Modifier
                            .padding(vertical = space.large)
                            .semantics {
                                contentDescription = "DragHandle"
                            }
                            .align(Alignment.Center),
                    color = chatColors.token.content.tertiary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        },
        modifier = Modifier
            .testTag("send_transcript_bottom_sheet")
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        SendTranscriptContent(
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }
                onDismiss()
            },
            onSubmit = { onSubmit(it) }
        )
    }

    if (showFullScreenLoading) {
        LoadingOverlayFullScreen(
            onClose = onDismiss,
            closeButtonText = stringResource(id = string.cancel)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SendTranscriptContent(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val validator = allOf(required, email)
    val emailText = rememberTextFieldState("")
    val confirmEmailText = rememberTextFieldState("")
    val confirmEmailFocused = remember { mutableStateOf(false) }
    val error = when {
        confirmEmailText.text.isEmpty() -> stringResource(string.error_required_field)
        !EMAIL_ADDRESS.matcher(confirmEmailText.text).matches() -> stringResource(string.error_email_validation)
        confirmEmailText.text != emailText.text -> stringResource(string.emails_do_not_match)
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.token.background.surface.subtle)
    ) {
        val submit = { onSubmit(emailText.text.toString()) }
        Column(
            modifier = Modifier
                .padding(start = space.large, top = space.large, end = space.large)
        ) {
            SendTranscriptHeader()
            EmailFields(
                emailText = emailText,
                confirmEmailText = confirmEmailText,
                confirmEmailFocused = confirmEmailFocused,
                validator = validator,
                error = error,
                onSubmit = submit,
            )
        }
        SendTranscriptBottomBar(
            onDismiss = onDismiss,
            onSubmit = submit,
            error = error
        )
    }
}

@Composable
private fun EmailFields(
    emailText: TextFieldState,
    confirmEmailText: TextFieldState,
    confirmEmailFocused: MutableState<Boolean>,
    validator: Requirement,
    error: String?,
    onSubmit: () -> Unit,
) {
    ChatTheme.TextField(
        label = stringResource(string.email),
        minimizedLabelBackground = Color.Unspecified,
        value = emailText,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("text_email"),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
        ),
        placeholder = stringResource(string.email_placeholder),
        validate = validator,
    )

    OutlinedTextField(
        state = confirmEmailText,
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(top = space.large)
            .testTag("text_confirm_email")
            .onFocusChanged { confirmEmailFocused.value = it.isFocused },
        label = {
            Text(
                text = stringResource(string.confirm_email),
                modifier =
                    Modifier
                        .background(Color.Unspecified)
                        .padding(horizontal = space.small),
                color = if (error != null && confirmEmailFocused.value) {
                    colorScheme.error
                } else {
                    Color.Unspecified
                }
            )
        },
        isError = error != null && confirmEmailFocused.value,
        supportingText = if (confirmEmailFocused.value) error?.let { { Text(it) } } else null,
        placeholder = {
            Text(text = stringResource(string.confirm_email_placeholder))
        },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        ),
        onKeyboardAction = {
            if (error == null) {
                onSubmit()
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Composable
private fun SendTranscriptHeader() {
    Column {
        Text(
            text = stringResource(string.chat_transcript),
            style = chatTypography.bottomSheetTitleText,
            modifier = Modifier
                .testTag("chat_transcript_title")
                .padding(start = space.large),
            color = chatColors.token.content.primary
        )
        Text(
            text = stringResource(string.chat_transcript_message),
            color = chatColors.token.content.secondary,
            style = chatTypography.listPickerBottomSheetSubtitleText,
            modifier = Modifier
                .padding(bottom = space.large, start = space.large)
                .testTag("list_picker_subtitle")
        )
    }
}

@Composable
private fun BoxScope.SendTranscriptBottomBar(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    error: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        HorizontalDivider(color = chatColors.token.border.default)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(chatColors.token.background.default)
        ) {
            BottomSheetButtonText(
                text = stringResource(string.cancel),
                enable = true,
                onClick = onDismiss
            )
            BottomSheetButtonText(
                text = stringResource(string.submit),
                enable = error == null,
                onClick = onSubmit
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewSendTranscript() {
    ChatTheme {
        SendTranscriptBottomSheet(
            onDismiss = {},
            onSubmit = {},
            showFullScreenLoading = true
        )
    }
}
