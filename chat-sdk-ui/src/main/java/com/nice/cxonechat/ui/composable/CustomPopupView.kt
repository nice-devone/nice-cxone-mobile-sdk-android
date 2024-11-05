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

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.ui.composable.generic.showActionSnackbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.main.ChatThreadViewModel
import com.nice.cxonechat.ui.main.ChatThreadViewModel.PopupActionState.PopupActionData
import com.nice.cxonechat.ui.main.ChatThreadViewModel.PopupActionState.PreviewPopupAction
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.Failure
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.Success
import com.nice.cxonechat.ui.util.toJsonElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@Composable
internal fun CustomPopUpView(
    snackbarHostState: SnackbarHostState,
    chatThreadViewModel: ChatThreadViewModel,
) {
    PopUpViewInternal(
        snackbarHostState = snackbarHostState,
        actionState = chatThreadViewModel.actionState,
        onPopupActionClicked = chatThreadViewModel::reportOnPopupActionClicked,
        onPopupAction = chatThreadViewModel::reportOnPopupAction,
        onPopupActionDisplayed = chatThreadViewModel::reportOnPopupActionDisplayed,
    )
}

@Composable
private inline fun <reified T : PopupActionData> PopUpViewInternal(
    snackbarHostState: SnackbarHostState,
    actionState: StateFlow<ChatThreadViewModel.PopupActionState>,
    crossinline onPopupActionClicked: (T) -> Unit,
    crossinline onPopupAction: (ChatThreadViewModel.ReportOnPopupAction, T) -> Unit,
    crossinline onPopupActionDisplayed: (T) -> Unit,
) {
    actionState
        .filterIsInstance<T>()
        .collectAsStateWithLifecycle(null)
        .value
        ?.let { action ->
            val rawVariables = action.variables
            runCatching {
                val variables = rawVariables.toJsonElement()
                Json.decodeFromJsonElement<CustomPopupData>(variables)
            }.onSuccess { popupData ->
                LaunchedEffect(popupData) {
                    snackbarHostState.showActionSnackbar(
                        message = popupData.bodyText,
                        actionLabel = popupData.action.actionText,
                        onAction = {
                            onPopupActionClicked(action)
                            onPopupAction(Success, action)
                        },
                        onDismiss = {
                            onPopupAction(Failure, action)
                        }
                    )
                    onPopupActionDisplayed(action)
                }
            }.onFailure {
                Text(it.message.orEmpty())
                Toast.makeText(
                    LocalContext.current,
                    "Unable to decode ReceivedOnPopupAction",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}

@Serializable
internal data class CustomPopupData(
    @SerialName("headingText") val headingText: String,
    @SerialName("bodyText") val bodyText: String,
    @SerialName("action") val action: ActionData,
)

@Serializable
internal data class ActionData(
    @SerialName("text") val actionText: String,
    @SerialName("url") val actionUrl: String,
)

// Preview

// Preview works in the interactive mode only
@Preview
@Composable
private fun PopUpViewPreview() {
    val actionState = remember { MutableStateFlow(nextPopupAction()) }
    ChatTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        ChatTheme.Scaffold(
            snackbarHostState = snackbarHostState,
        ) {
            PopUpViewInternal<PreviewPopupAction>(
                snackbarHostState = snackbarHostState,
                actionState = actionState,
                onPopupActionClicked = {},
                onPopupAction = { _, _ -> },
                onPopupActionDisplayed = {}
            )
        }
    }
}

private fun nextPopupAction() = PreviewPopupAction(
    variables = mapOf(
        "headingText" to "Heading",
        "bodyText" to "Body",
        "action" to mapOf(
            "text" to "Action",
            "url" to "https://example.com"
        )
    ),
    metadata = object {}
)
