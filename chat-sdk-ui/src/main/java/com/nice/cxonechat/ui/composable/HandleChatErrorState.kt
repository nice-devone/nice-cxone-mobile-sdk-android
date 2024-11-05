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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.util.Ignored
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Display chat errors to the user.
 * If the error [AuthorizationError] user will be notified that the chat will be terminated and the [onTerminalError] will be called.
 */
@Composable
internal fun HandleChatErrorState(
    snackbarHostState: SnackbarHostState,
    chatErrorStateFlow: Flow<RuntimeChatException>,
    onTerminalError: () -> Unit,
) {
    val context = LocalContext.current
    when (val error = chatErrorStateFlow.collectAsStateWithLifecycle(null).value) {
        null -> Ignored

        is AuthorizationError -> AlertDialog(
            onDismissRequest = {
                onTerminalError()
            },
            text = { Text(stringResource(R.string.chat_state_error_default_message)) },
            confirmButton = {
                TextButton(onClick = onTerminalError) {
                    Text(stringResource(R.string.chat_state_error_action_close))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        )

        else -> LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.chat_state_error_default_message),
                actionLabel = context.getString(R.string.dismiss),
                duration = Short,
            )
        }
    }
}

@Composable
@Preview
private fun HandleChatErrorPreview() {
    ChatTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val chatErrorStateFlow: MutableStateFlow<RuntimeChatException?> = remember { MutableStateFlow(getServerErr()) }
        val filteredFlow = remember { chatErrorStateFlow.filterNotNull() }
        ChatTheme.Scaffold(
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
                HandleChatErrorState(
                    snackbarHostState = snackbarHostState,
                    chatErrorStateFlow = filteredFlow,
                    onTerminalError = {
                        chatErrorStateFlow.value = getServerErr()
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
