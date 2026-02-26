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

import com.nice.cxonechat.internal.model.ApiErrorModel
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import kotlinx.serialization.json.Json

/**
 * Helper for getting transaction tokens via API.
 */
internal object TransactionTokenHelper {
    /**
     * Calls the getTransactionToken API and handles the response.
     *
     * @param entrails ChatEntrails containing the AuthService
     * @param brandId Brand ID as string
     * @param channelId Channel ID as string
     * @param visitorId Visitor ID as string
     * @param tokenRequestBody The request body
     * @return [TransactionTokenResult] containing the outcome
     */
    internal fun getTransactionToken(
        entrails: ChatEntrails,
        brandId: String,
        channelId: String,
        visitorId: String,
        tokenRequestBody: TokenRequestBody,
    ): TransactionTokenResult {
        val response = entrails.authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenRequestBody
        ).execute()
        return if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val apiError = errorBody?.let {
                runCatching { Json.decodeFromString<ApiErrorModel?>(it) }.getOrNull()
            }
            val errorMessage = apiError?.error?.errorMessage
            TransactionTokenResult.Error(errorMessage ?: "Failed to get transaction token: ${response.code()} ${response.message()}")
        } else {
            response.body()?.let { TransactionTokenResult.Success(it) }
                ?: TransactionTokenResult.Error("Failed to get transaction token: empty response body")
        }
    }

    internal sealed class TransactionTokenResult {
        data class Success(val transactionTokenModel: TransactionTokenModel) : TransactionTokenResult()
        data class Error(val message: String) : TransactionTokenResult()
    }
}
