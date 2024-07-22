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

package com.nice.cxonechat.ui.model

import android.content.Context
import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure.GENERAL_FAILURE
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure.REASON_PRECHAT_SURVEY_REQUIRED
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure.REASON_PRECHAT_SURVEY_VALIDATION
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure.REASON_THREADS_REFRESH_REQUIRED
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure.REASON_THREAD_CREATION_FORBIDDEN
import com.nice.cxonechat.ui.model.CreateThreadResult.Success

/**
 * Possible results of thread creation action.
 */
internal sealed interface CreateThreadResult {
    object Success : CreateThreadResult

    @Suppress("ClassName")
    sealed interface Failure : CreateThreadResult {
        object REASON_THREADS_REFRESH_REQUIRED : Failure
        object REASON_THREAD_CREATION_FORBIDDEN : Failure
        object REASON_PRECHAT_SURVEY_REQUIRED : Failure
        class REASON_PRECHAT_SURVEY_VALIDATION(val reason: String?) : Failure
        object GENERAL_FAILURE : Failure
    }
}

/**
 * Utility transformation method for conversion of [Result] to [CreateThreadResult], assuming that the result is
 * product of [runCatching] executed over [com.nice.cxonechat.ChatThreadsHandler.thread] method.
 */
internal fun <T> Result<T>.foldToCreateThreadResult(): CreateThreadResult = fold(
    onSuccess = { Success },
    onFailure = { throwable ->
        when (throwable) {
            is UnsupportedChannelConfigException -> REASON_THREAD_CREATION_FORBIDDEN
            is MissingThreadListFetchException -> REASON_THREADS_REFRESH_REQUIRED
            is MissingPreChatCustomFieldsException -> REASON_PRECHAT_SURVEY_REQUIRED
            is InvalidCustomFieldValue -> REASON_PRECHAT_SURVEY_VALIDATION(throwable.message)
            else -> GENERAL_FAILURE
        }
    }
)

internal fun Context.describe(result: CreateThreadResult.Failure) = when(result) {
    REASON_THREADS_REFRESH_REQUIRED -> getString(string.warning_threads_refresh_required)
    REASON_THREAD_CREATION_FORBIDDEN -> getString(string.warning_thread_creation_forbidden)
    GENERAL_FAILURE -> getString(string.warning_general_failure)
    REASON_PRECHAT_SURVEY_REQUIRED -> getString(string.warning_missing_prechat_survey_response_failure)
    is REASON_PRECHAT_SURVEY_VALIDATION -> result.reason ?: getString(string.warning_invalid_fields)
}
