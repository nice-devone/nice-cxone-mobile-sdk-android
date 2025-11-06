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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.enums.ErrorType.CreateOrUpdateVisitorFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal data class RetryParams<T>(
    val action: () -> T,
    val onSuccess: (T) -> Unit,
    val onFailure: (Throwable) -> Unit,
)

internal fun createVisitorRetryParams(
    createOrUpdateVisitor: Call<Void>,
    callback: Callback<Void>,
    chatStateListener: ChatStateListener?,
): RetryParams<Pair<Call<Void>, Response<Void>>> {
    return RetryParams(
        action = {
            val response = createOrUpdateVisitor.execute()
            if (response.isSuccessful || response.code() in 400..499) {
                Pair(createOrUpdateVisitor, response)
            } else {
                throw ServerCommunicationError(response.toString())
            }
        },
        onSuccess = { (call, response) ->
            callback.onResponse(call, response)
        },
        onFailure = { throwable ->
            callback.onFailure(createOrUpdateVisitor, throwable)
            chatStateListener?.onChatRuntimeException(
                ServerCommunicationError(CreateOrUpdateVisitorFailed.value)
            )
        }
    )
}
