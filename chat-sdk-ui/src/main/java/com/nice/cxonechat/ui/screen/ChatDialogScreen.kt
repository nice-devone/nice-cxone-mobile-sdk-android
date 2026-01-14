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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.describe
import com.nice.cxonechat.ui.composable.theme.Alert
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.None
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Preparing
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Survey
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.ThreadCreationFailed
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        val loading = stringResource(string.loading)
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("preparing_dialog")
                .semantics {
                    contentDescription = loading
                },
            contentAlignment = Alignment.Center
        ) {
            LoadingContent(cancelAction)
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
