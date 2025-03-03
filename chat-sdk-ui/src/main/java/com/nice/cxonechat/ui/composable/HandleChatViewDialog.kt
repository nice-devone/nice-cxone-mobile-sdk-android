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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.ui.PreChatSurveyScreen
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ChatTheme.typography
import com.nice.cxonechat.ui.main.ChatViewModel.DialogState
import com.nice.cxonechat.ui.main.ChatViewModel.DialogState.None
import com.nice.cxonechat.ui.main.ChatViewModel.DialogState.Preparing
import com.nice.cxonechat.ui.main.ChatViewModel.DialogState.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.DialogState.ThreadCreationFailed
import com.nice.cxonechat.ui.model.describe
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.util.showAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Display dialog used for thread creation based on the state of the [com.nice.cxonechat.ui.main.ChatViewModel.DialogState] flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HandleChatViewDialog(
    dialogShownFlow: StateFlow<DialogState>,
    cancelAction: () -> Unit,
    submitAction: (Sequence<PreChatResponse>) -> Unit = {},
    retryAction: () -> Unit,
) {
    val context = LocalContext.current
    when (val dialog = dialogShownFlow.collectAsState().value) {
        None -> Ignored
        Preparing -> PreparingDialog(cancelAction)
        is Survey -> PreChatSurveyScreen(
            survey = dialog.survey,
            onCancel = cancelAction,
            onValidSurveySubmission = submitAction,
        )

        is ThreadCreationFailed -> context.showAlert(context.describe(dialog.failure), onClick = retryAction)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
private fun PreparingDialog(cancelAction: () -> Unit = {}) {
    Dialog(onDismissRequest = cancelAction, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card(
            shape = ChatTheme.shapes.large
        ) {
            val center = Modifier.align(Alignment.CenterHorizontally)
            Text(
                modifier = center.padding(space.large),
                text = stringResource(string.preparing),
                style = typography.titleMedium
            )
            LoadingIndicator(
                modifier = center
                    .padding(space.xxl)
                    .size(space.loadingIndicatorSize),
                color = colorScheme.primary
            )
            TextButton(onClick = cancelAction, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(string.cancel))
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
                // MaterialAlertDialogBuilder cannot be previewed
            }
            HandleChatViewDialog(
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
