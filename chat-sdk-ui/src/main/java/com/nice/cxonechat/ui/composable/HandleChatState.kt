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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.generic.showActionSnackbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.util.Ignored
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Display a snackbar based on the current chat state.
 *
 * @param snackbarHostState The [SnackbarHostState] to show the snackbar in.
 * @param chatStateFlow The [StateFlow] of the current chat state.
 * @param onConnectChatAction The action to invoke when chat should attempt to connect.
 * @param onReadyAction The action to invoke when chat is ready.
 * @param onFinishAction The action to invoke when chat is has successfully connected.
 * @param onOfflineAction The action to invoke when chat is offline.
 */
@Composable
internal fun HandleChatState(
    snackbarHostState: SnackbarHostState,
    chatStateFlow: StateFlow<ChatState>,
    onConnectChatAction: () -> Unit,
    onReadyAction: () -> Unit,
    onFinishAction: () -> Unit,
    onOfflineAction: () -> Unit,
) {
    val context = LocalContext.current
    val state by chatStateFlow.collectAsStateWithLifecycle(null)
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(state, lifecycleState) {
        snackbarHostState.currentSnackbarData?.dismiss()
        when (state) {
            // Initial and Preparing states are handled separately
            null, Initial, Preparing -> Ignored

            // if the chat is (or becomes) prepared, then start a connect attempt, but this shouldn't happen when the ui is not visible
            Prepared -> if (lifecycleState.isAtLeast(State.RESUMED)) {
                onConnectChatAction()
            }

            Connecting -> snackbarHostState.showCancellableSnackbar(
                message = context.getString(R.string.chat_state_connecting),
                actionLabel = context.getString(R.string.cancel),
                onAction = onFinishAction,
            )

            Connected -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.chat_state_connected),
                duration = Short,
            )

            Ready -> if (lifecycleState.isAtLeast(State.RESUMED)) {
                snackbarHostState.showSnackbar(
                    "SDK ready",
                    duration = Short,
                ).also {
                    onReadyAction()
                }
            }

            // TODO DE-87750: Figure out how to handle this properly.
            Offline -> {
                onOfflineAction()
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.chat_state_offline),
                )
                if (lifecycleState.isAtLeast(State.RESUMED)) {
                    // Automatically refresh the chat state when the UI is visible
                    onConnectChatAction()
                }
            }

            ConnectionLost -> snackbarHostState.showActionSnackbar(
                message = context.getString(R.string.chat_state_connection_lost),
                actionLabel = context.getString(R.string.chat_state_connection_lost_action_reconnect),
                onAction = onConnectChatAction
            )
        }
    }
}

@Composable
@Preview
private fun PreviewHandleChatState() {
    ChatTheme {
        val chatStateFlow = remember { MutableStateFlow<ChatState>(Prepared) }
        val chatState by chatStateFlow.collectAsStateWithLifecycle(Initial)
        val snackbarHostState = remember { SnackbarHostState() }
        val lifecycleOwner = LocalLifecycleOwner.current

        ChatTheme.Scaffold(
            snackbarHostState = snackbarHostState,
        ) {
            Column(modifier = Modifier.padding(it)) {
                Text("Chat State: $chatState")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                ) {
                    items(ChatState.entries.count(), key = { index -> ChatState.entries[index] }) { index ->
                        val state = ChatState.entries[index]
                        TextButton(onClick = { chatStateFlow.value = state }) {
                            Text(state.name)
                        }
                    }
                }
            }
            HandleChatState(
                snackbarHostState = snackbarHostState,
                chatStateFlow = chatStateFlow,
                onConnectChatAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onConnectChatAction") },
                onReadyAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onReadyAction") },
                onFinishAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onFinishAction") },
                onOfflineAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onOfflineAction") },
            )
        }
    }
}

private fun showSnackbar(
    lifecycleOwner: LifecycleOwner,
    snackbarHostState: SnackbarHostState,
    message: String,
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(State.STARTED) {
            snackbarHostState.showSnackbar(message, duration = Short)
        }
    }
}
