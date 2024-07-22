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

package com.nice.cxonechat.ui

import android.widget.Toast
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.customvalues.CustomValueItem
import com.nice.cxonechat.ui.customvalues.toPreChatResponse
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.util.isSurveyResponseValid

@Composable
internal fun PreChatSurveyDialog(
    survey: PreChatSurvey,
    onCancel: () -> Unit,
    onValidSurveySubmission: (Sequence<PreChatResponse>) -> Unit,
) {
    val context = LocalContext.current
    val surveyFields = remember {
        survey.fields.map { definition -> CustomValueItem(definition) }.toList()
    }
    Surface {
        EditCustomValuesDialog(
            title = survey.name,
            fields = surveyFields,
            onCancel = onCancel,
            onConfirm = { customValueItems ->
                val responses = customValueItems
                    .asSequence()
                    .mapNotNull(CustomValueItem<*, *>::toPreChatResponse)
                if (isSurveyResponseValid(survey, responses.asIterable())) {
                    onValidSurveySubmission(responses)
                } else {
                    Toast.makeText(context, string.warning_missing_prechat_survey_response, Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
