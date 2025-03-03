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

package com.nice.cxonechat.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.customvalues.CustomValueItem
import com.nice.cxonechat.ui.customvalues.CustomValueItemList
import com.nice.cxonechat.ui.customvalues.toPreChatResponse
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.util.isSurveyResponseValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreChatSurveyScreen(
    survey: PreChatSurvey,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onCancel: () -> Unit,
    onValidSurveySubmission: (Sequence<PreChatResponse>) -> Unit,
) {
    val context = LocalContext.current
    val surveyFields = remember {
        survey.fields.map { definition -> CustomValueItem(definition) }.toList()
    }
    var canSubmit by remember { mutableStateOf(isResponseValid(survey, emptyList())) }
    EditCustomValuesScreen(
        title = survey.name,
        fields = surveyFields,
        canSubmit = canSubmit,
        onUpdated = { canSubmit = isResponseValid(survey, it) },
        sheetState = sheetState,
        onCancel = onCancel,
        onConfirm = validateOnConfirm(survey, onValidSurveySubmission, context)
    )
}

internal fun isResponseValid(
    survey: PreChatSurvey,
    customValueItems: CustomValueItemList,
): Boolean {
    val responses = customValueItems
        .mapNotNull(CustomValueItem<*, *>::toPreChatResponse)
    return isSurveyResponseValid(survey, responses.asIterable())
}

private fun validateOnConfirm(
    survey: PreChatSurvey,
    onValidSurveySubmission: (Sequence<PreChatResponse>) -> Unit,
    context: Context,
) = { customValueItems: CustomValueItemList ->
    val responses = customValueItems
        .asSequence()
        .mapNotNull(CustomValueItem<*, *>::toPreChatResponse)
    if (isSurveyResponseValid(survey, responses.asIterable())) {
        onValidSurveySubmission(responses)
    } else {
        Toast.makeText(context, string.warning_missing_prechat_survey_response, Toast.LENGTH_LONG).show()
    }
}
