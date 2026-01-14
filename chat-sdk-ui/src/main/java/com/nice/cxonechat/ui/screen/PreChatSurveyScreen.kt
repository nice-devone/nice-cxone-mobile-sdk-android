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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.EditCustomValuesScreen
import com.nice.cxonechat.ui.domain.model.CustomValueItem
import com.nice.cxonechat.ui.domain.model.CustomValueItemList
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.domain.model.toPreChatResponse
import com.nice.cxonechat.ui.util.isSurveyResponseValid
import com.nice.cxonechat.ui.util.showToast

/**
 * Displays a pre-chat survey screen where users can fill out a survey before starting a chat.
 *
 * @param survey The [PreChatSurvey] to be displayed.
 * @param modifier [Modifier] to be applied to the screen.
 * @param sheetState [SheetState] for the bottom sheet.
 * @param onCancel Callback invoked when the user cancels the survey.
 * @param onValidSurveySubmission Callback invoked with valid survey responses when the user submits the survey.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreChatSurveyScreen(
    survey: PreChatSurvey,
    modifier: Modifier = Modifier,
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
        modifier = modifier.testTag("prechat_survey"),
        onCancel = onCancel,
        onConfirm = validateOnConfirm(survey, onValidSurveySubmission, context)
    )
}

/**
 * Checks if the provided survey responses are valid according to the given survey.
 *
 * @param survey The [PreChatSurvey] to validate against.
 * @param customValueItems The list of [CustomValueItem]s representing user responses.
 * @return `true` if the responses are valid, `false` otherwise.
 */
internal fun isResponseValid(
    survey: PreChatSurvey,
    customValueItems: CustomValueItemList,
): Boolean {
    val responses = customValueItems
        .mapNotNull(CustomValueItem<*, *>::toPreChatResponse)
    return isSurveyResponseValid(survey, responses.asIterable())
}

/**
 * Returns a lambda function that validates the survey responses and invokes the callback with valid responses.
 *
 * @param survey The [PreChatSurvey] to validate against.
 * @param onValidSurveySubmission Callback invoked with valid survey responses.
 * @param context The [Context] to show a toast message if validation fails.
 * @return A lambda function that takes a list of [CustomValueItem]s and validates them.
 */
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
        context.showToast(string.warning_missing_prechat_survey_response, Toast.LENGTH_LONG)
    }
}
