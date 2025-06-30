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

package com.nice.cxonechat.ui.screen

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.describe
import com.nice.cxonechat.ui.composable.theme.Alert
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.DefaultColors.overlayBackground
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.None
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Preparing
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Survey
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.ThreadCreationFailed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Composable function that displays a dialog based on the current state of the chat.
 *
 * @param dialogShownFlow Flow that emits the current dialog state.
 * @param modifier Modifier to be applied to the dialog.
 * @param cancelAction Action to perform when the dialog is cancelled.
 * @param submitAction Action to perform when a valid survey is submitted.
 * @param retryAction Action to perform when retrying after a failure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatDialogScreen(
    dialogShownFlow: StateFlow<DialogState>,
    modifier: Modifier = Modifier,
    cancelAction: () -> Unit,
    submitAction: (Sequence<PreChatResponse>) -> Unit = {},
    retryAction: () -> Unit,
) {
    when (val dialog = dialogShownFlow.collectAsState().value) {
        None -> {}
        Preparing -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LoadingOverlayFullScreen(modifier = modifier, onClose = cancelAction)
        } else {
            // Workaround for DE-134115, where the UI is not properly updated
            PreparingDialog(
                modifier = modifier,
                cancelAction = cancelAction
            )
        }

        is Survey -> PreChatSurveyScreen(
            modifier = modifier,
            survey = dialog.survey,
            onCancel = cancelAction,
            onValidSurveySubmission = submitAction,
        )

        is ThreadCreationFailed -> ChatTheme.Alert(
            message = describe(dialog.failure),
            onDismiss = retryAction,
            dismissLabel = stringResource(string.ok)
        )
    }
}

/**
 * Composable function that displays a full-screen loading overlay with a close button after a delay.
 *
 * @param modifier Modifier to be applied to the overlay.
 * @param onClose Action to perform when the close button is clicked.
 */
@Composable
private fun LoadingOverlayFullScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    val loading = stringResource(string.loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBackground)
            .zIndex(1f)
            .testTag("preparing_dialog")
            .semantics {
                contentDescription = loading
            }
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        LoadingContent(onClose)
    }
}

/**
 * Composable function that displays a loading screen with a progress indicator and a close button.
 *
 * @param onClose Action to perform when the close button is clicked.
 */
@Composable
private fun LoadingContent(onClose: () -> Unit = {}) {
    val showCloseButton = remember { mutableStateOf(false) }
    val delayMs = integerResource(R.integer.loading_close_button_delay_ms).toLong()

    LaunchedEffect(Unit) {
        delay(delayMs) // 20 seconds wait to show close button
        showCloseButton.value = true
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary,
            strokeWidth = space.small
        )
        Spacer(modifier = Modifier.height(space.large))
        Text(
            text = stringResource(string.loading),
            color = colorScheme.onBackground.copy(alpha = 0.5f),
            style = chatTypography.loadingScreenText
        )

        if (showCloseButton.value) {
            LoadingDelayView(
                onClose = onClose
            )
        }
    }
}

/**
 * Composable function that displays a message and a close button after a delay.
 *
 * @param onClose Action to perform when the close button is clicked.
 */
@Composable
private fun LoadingDelayView(
    onClose: () -> Unit = {},
) {
    val closeButtonText = stringResource(string.loading_close_button_text)

    Spacer(modifier = Modifier.height(space.large))
    Text(
        modifier = Modifier.alpha(0.5f),
        text = stringResource(string.loading_delay_message),
        color = colorScheme.onBackground,
        style = chatTypography.loadingScreenText
    )
    Spacer(modifier = Modifier.height(space.medium))
    Button(
        modifier = Modifier
            .testTag("close_button")
            .semantics { contentDescription = closeButtonText },
        onClick = onClose,
        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
        shape = chatShapes.chip
    ) {
        Text(text = closeButtonText, color = colorScheme.onPrimary)
    }
}

/**
 * Composable function that displays a dialog while preparing the chat thread.
 *
 * @param modifier Modifier to be applied to the dialog.
 * @param cancelAction Action to perform when the dialog is cancelled.
 */
@Composable
@Preview
private fun PreparingDialog(
    modifier: Modifier = Modifier,
    cancelAction: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = cancelAction,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        val loading = stringResource(string.loading)
        Card(
            shape = ChatTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = modifier
                .testTag("preparing_dialog")
                .semantics {
                    contentDescription = loading
                },
        ) {
            Column(modifier.padding(space.xxl)) {
                LoadingContent(cancelAction)
            }
        }
    }
}

@Composable
@Preview
private fun ChatDialogPreview() {
    val dialogFlow: MutableStateFlow<DialogState> = remember { MutableStateFlow(None) }
    ChatTheme {
        Column {
            Row {
                TextButton(onClick = {
                    dialogFlow.value = None
                }) {
                    Text("None")
                }
                TextButton(onClick = { dialogFlow.value = Survey(Survey) }) {
                    Text("Survey")
                }
            }
            ChatDialogScreen(
                dialogShownFlow = dialogFlow,
                cancelAction = { dialogFlow.value = None },
                submitAction = { dialogFlow.value = None },
                retryAction = { dialogFlow.value = None },
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayFullPreview() {
    LoadingOverlayFullScreen(
        modifier = Modifier
            .fillMaxSize(),
        onClose = { /* No action needed for preview */ }
    )
}

private object Survey : PreChatSurvey {
    override val name = "PreChat Survey"
    override val fields: FieldDefinitionList = sequenceOf(
        object : FieldDefinition.Text {
            override val fieldId = UUID.randomUUID().toString()
            override val label = "Name"
            override val isRequired = false
            override val isEMail = false

            override fun validate(value: String) = Unit
        },
        object : FieldDefinition.Text {
            override val fieldId = UUID.randomUUID().toString()
            override val label = "EMail"
            override val isRequired = true
            override val isEMail = true

            override fun validate(value: String) = Unit
        }
    )
}
