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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.enums.ErrorType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model for error event pushed from server.
 *
 * @property error Details about the error.
 */
@Serializable
internal data class ErrorModel(
    @SerialName("error")
    val error: Error,
) {
    /**
     * Error details.
     *
     * @property errorCode One of predefined [ErrorType]s.
     * @property transactionId Id of transaction which has triggered the error, usable for tracking down the cause in
     * server logs.
     */
    @Serializable
    internal data class Error(
        @SerialName("errorCode")
        val errorCode: ErrorType,
        @SerialName("transactionId")
        val transactionId: String,
    )
}
