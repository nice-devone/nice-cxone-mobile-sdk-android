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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.enums.ErrorType.CreateOrUpdateVisitorFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resumeWithException

internal data class RetryParams<T>(
    val action: suspend () -> T,
    val onSuccess: (T) -> Unit,
    val onFailure: (Throwable) -> Unit,
)

internal fun createVisitorRetryParams(
    prepareCreateOrUpdateVisitor: () -> Call<Void>,
    callback: Callback<Void>,
    chatStateListener: ChatStateListener?,
): RetryParams<Pair<Call<Void>, Response<Void>>> {
    // Track the most recent Call so onFailure can reference the actual instance that failed.
    var lastCall: Call<Void>? = null
    return RetryParams(
        action = {
            val createOrUpdateVisitor = prepareCreateOrUpdateVisitor()
            lastCall = createOrUpdateVisitor
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    // Cancellation (e.g. an ordinary chat close during the retry delay) is not a
                    // failure — just release the in-flight Call. Reporting callback.onFailure /
                    // onChatRuntimeException here surfaced a spurious ServerCommunicationError to
                    // integrators on normal shutdown. Genuine failures are reported by the enqueue
                    // callback and RetryParams.onFailure instead.
                    createOrUpdateVisitor.cancel()
                }
                createOrUpdateVisitor.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful || response.code() in 400..499) {
                            val value = Pair(createOrUpdateVisitor, response)
                            continuation.resume(value) { cause, _, _ ->
                                createOrUpdateVisitor.cancel()
                                if (cause !is CancellationException) {
                                    chatStateListener?.onChatRuntimeException(
                                        ServerCommunicationError(CreateOrUpdateVisitorFailed.value)
                                    )
                                }
                            }
                        } else {
                            continuation.resumeWithException(ServerCommunicationError(response.toString()))
                        }
                    }

                    override fun onFailure(call: Call<Void?>, t: Throwable) {
                        continuation.resumeWithException(ServerCommunicationError(CreateOrUpdateVisitorFailed.value))
                    }
                })
            }
        },
        onSuccess = { (call, response) ->
            callback.onResponse(call, response)
        },
        onFailure = { throwable ->
            callback.onFailure(lastCall ?: prepareCreateOrUpdateVisitor(), throwable)
            chatStateListener?.onChatRuntimeException(
                ServerCommunicationError(CreateOrUpdateVisitorFailed.value)
            )
        }
    )
}
