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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.ui.PreChatSurveyDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.ThreadCreationFailed
import com.nice.cxonechat.ui.model.describe
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.util.showAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Display dialog used for thread creation based on the state of the [com.nice.cxonechat.ui.main.ChatViewModel.Dialogs] flow.
 */
@Composable
internal fun HandleChatViewDialog(
    dialogShownFlow: StateFlow<Dialogs>,
    cancelAction: () -> Unit,
    submitAction: (Sequence<PreChatResponse>) -> Unit = {},
    retryAction: () -> Unit,
) {
    val context = LocalContext.current
    when (val dialog = dialogShownFlow.collectAsState().value) {
        None -> Ignored
        is Survey -> PreChatSurveyDialog(
            survey = dialog.survey,
            onCancel = cancelAction,
            onValidSurveySubmission = submitAction,
        )

        is ThreadCreationFailed -> context.showAlert(context.describe(dialog.failure), onClick = retryAction)
    }
}

@Composable
@Preview
private fun ChatDialogPreview() {
    val dialogFlow: MutableStateFlow<Dialogs> = remember { MutableStateFlow(None) }
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
