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

package com.nice.cxonechat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.ErrorAlertDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.util.ErrorGroup
import com.nice.cxonechat.ui.util.ErrorGroup.DO_NOTHING
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Display chat errors to the user.
 * If the error [AuthorizationError] user will be notified that the chat will be terminated and the [onTerminalError] will be called.
 *
 * @param modifier [Modifier] to be applied to the dialog.
 * @param onTerminalError Callback to be invoked when the error is terminal, e.g. when the user acknowledges the error.
 */
@Composable
internal fun ChatErrorScreen(
    modifier: Modifier = Modifier,
    onTerminalError: () -> Unit,
) {
    val chatStateViewModel = koinActivityViewModel<ChatStateViewModel>()
    val chatErrorState by chatStateViewModel.chatErrorState.collectAsState()

    when (chatErrorState.errorGroup) {
        DO_NOTHING -> Ignored

        ErrorGroup.HIGH ->
            ChatTheme.ErrorAlertDialog(
                title = stringResource(R.string.error_dialog_title),
                body = stringResource(R.string.error_dialog_body),
                buttonText = stringResource(R.string.error_dialog_close_button_text),
                onConfirmClick = onTerminalError,
                modifier = modifier
            )

        ErrorGroup.LOW ->
            ChatTheme.ErrorAlertDialog(
                title = stringResource(R.string.error_dialog_title),
                body = stringResource(R.string.error_dialog_body),
                buttonText = stringResource(R.string.error_dialog_continue_button),
                onConfirmClick = {
                    chatStateViewModel.resetError()
                },
                modifier = modifier
            )

        ErrorGroup.LOW_SPECIFIC ->
            ChatTheme.ErrorAlertDialog(
                title = chatErrorState.title ?: stringResource(R.string.error_dialog_title),
                body = chatErrorState.message ?: stringResource(R.string.error_dialog_body),
                buttonText = stringResource(R.string.error_dialog_continue_button),
                onConfirmClick = {
                    chatStateViewModel.resetError()
                },
                modifier = modifier
            )
    }
}

@Composable
@Preview
private fun HandleChatErrorPreview() {
    ChatTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val chatErrorStateFlow: MutableStateFlow<RuntimeChatException?> = remember { MutableStateFlow(getServerErr()) }
        ChatTheme.Scaffold(
            modifier = Modifier.systemBarsPadding(),
            snackbarHostState = snackbarHostState,
        ) {
            Column {
                Row {
                    TextButton(onClick = { chatErrorStateFlow.value = getServerErr() }) {
                        Text("Server error")
                    }
                    TextButton(onClick = { chatErrorStateFlow.value = getAuthErr() }) {
                        Text("Auth error")
                    }
                }
                ChatErrorScreen(
                    onTerminalError = {
                        chatErrorStateFlow.value = null
                    }
                )
            }
        }
    }
}

private fun getAuthErr(): AuthorizationError = AuthorizationError::class.java.declaredConstructors
    .first()
    .newInstance("Auth err") as AuthorizationError

private fun getServerErr(): ServerCommunicationError = ServerCommunicationError::class.java.declaredConstructors
    .first()
    .newInstance("Server err") as ServerCommunicationError
