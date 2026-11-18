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
import com.nice.cxonechat.ChatState.SdkNotSupported
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.util.Ignored
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Perform action based on the current chat state and lifecycle state.
 *
 * @param chatStateFlow The [StateFlow] of the current chat state.
 * @param isAutoReconnectBlocked Whether a non-dismissible terminal error dialog (see
 * [com.nice.cxonechat.ui.util.ErrorGroup.HIGH]/[com.nice.cxonechat.ui.util.ErrorGroup.HIGH_SPECIFIC]) is currently
 * displayed. While true, auto-reconnect is suppressed — otherwise the SDK would keep resubmitting the same
 * already-rejected credential on every recomposition for as long as the dialog stays open (DE-178452). This is a
 * [LaunchedEffect] key of the connect-only effect below, but not of the one-shot [onReadyAction]/[onOfflineAction]
 * effect: a terminal error can clear (e.g. a later, lower-severity error overwrites the single-slot error state)
 * without [ChatState] or lifecycle changing, and the connect attempt skipped while it was showing must resume then
 * — but [Ready]/[Offline] must not re-fire their one-shot actions purely because this flag flipped.
 * @param onConnectChatAction The action to invoke when chat should attempt to connect.
 * @param onReadyAction The action to invoke when chat is ready.
 * @param onOfflineAction The action to invoke when chat is offline.
 */
@Composable
internal fun ChatStateEffect(
    chatStateFlow: StateFlow<ChatState>,
    isAutoReconnectBlocked: Boolean,
    onConnectChatAction: () -> Unit,
    onReadyAction: () -> Unit,
    onOfflineAction: () -> Unit,
) {
    val state by chatStateFlow.collectAsStateWithLifecycle(null)
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    // One-shot actions: keyed on state + lifecycle only, so they never re-fire from the error flag alone.
    LaunchedEffect(state, lifecycleState) {
        when (state) {
            Ready -> if (lifecycleState.isAtLeast(State.RESUMED)) {
                onReadyAction()
            }

            // TODO DE-87750: Figure out how to handle this properly.
            Offline -> onOfflineAction()

            else -> Ignored
        }
    }

    /*
     * Auto-connect: keyed on the error flag too, so it resumes as soon as a terminal error dialog clears,
     * even if state and lifecycle stay unchanged in the meantime.
     */
    LaunchedEffect(state, lifecycleState, isAutoReconnectBlocked) {
        if (!lifecycleState.isAtLeast(State.RESUMED) || isAutoReconnectBlocked) return@LaunchedEffect
        when (state) {
            /*
             * If the chat is (or becomes) prepared/disconnected, then start a connect attempt,
             * but this shouldn't happen when the ui is not visible, nor while a terminal error
             * dialog is blocking the screen (the credential that caused it was already rejected).
             * Connecting is also included here to show the loading overlay when the SDK is
             * autonomously reconnecting after a network drop. Offline is included so the chat state
             * gets automatically refreshed when the UI is visible.
             */
            Prepared, ConnectionLost, Connecting, Offline -> onConnectChatAction()

            // These states do not require any action, so we ignore them
            null, Initial, Preparing, Connected, Ready, SdkNotSupported -> Ignored
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
            ChatStateEffect(
                chatStateFlow = chatStateFlow,
                isAutoReconnectBlocked = false,
                onConnectChatAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onConnectChatAction") },
                onReadyAction = { showSnackbar(lifecycleOwner, snackbarHostState, "onReadyAction") },
            ) { showSnackbar(lifecycleOwner, snackbarHostState, "onOfflineAction") }
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
