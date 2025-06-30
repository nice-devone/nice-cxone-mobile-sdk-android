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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.domain.model.CreateThreadResult

@Composable
internal fun describe(result: CreateThreadResult.Failure) = when (result) {
    CreateThreadResult.Failure.REASON_THREADS_REFRESH_REQUIRED -> stringResource(R.string.warning_threads_refresh_required)
    CreateThreadResult.Failure.REASON_THREAD_CREATION_FORBIDDEN -> stringResource(R.string.warning_thread_creation_forbidden)
    CreateThreadResult.Failure.GENERAL_FAILURE -> stringResource(R.string.warning_general_failure)
    CreateThreadResult.Failure.REASON_PRECHAT_SURVEY_REQUIRED -> stringResource(R.string.warning_missing_prechat_survey_response_failure)
    is CreateThreadResult.Failure.REASON_PRECHAT_SURVEY_VALIDATION -> result.reason ?: stringResource(R.string.warning_invalid_fields)
}
