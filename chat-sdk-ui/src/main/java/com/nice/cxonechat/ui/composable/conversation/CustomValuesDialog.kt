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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.EditCustomValuesScreen
import com.nice.cxonechat.ui.domain.model.mergeWithCustomField
import com.nice.cxonechat.ui.screen.isResponseValid
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomValuesDialog(
    chatViewModel: ChatThreadViewModel,
) {
    val survey = chatViewModel.preChatSurvey
    var canSubmit by remember { mutableStateOf(survey?.let { isResponseValid(it, emptyList()) } ?: false) }
    EditCustomValuesScreen(
        title = stringResource(R.string.edit_custom_field_title),
        fields = survey
            ?.fields
            .orEmpty()
            .mergeWithCustomField(
                chatViewModel.customValues
            ),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onCancel = chatViewModel::cancelEditingCustomValues,
        onConfirm = chatViewModel::confirmEditingCustomValues,
        canSubmit = canSubmit,
        onUpdated = { values ->
            canSubmit = if (survey != null) isResponseValid(survey, values) else false
        },
        modifier = Modifier.testTag("custom_values_dialog"),
    )
}
