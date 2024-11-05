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

package com.nice.cxonechat.sample.data.models

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.UserName
import kotlinx.serialization.Serializable

/**
 * Saved chat settings.
 *
 * @param sdkConfiguration SDK Configuration, server details.
 * @param authorization saved authorization credentials.
 * @param userName saved user name.
 * @param customerId saved CustomerId.
 */
@Serializable
data class ChatSettings(
    val sdkConfiguration: SdkConfiguration? = null,
    val authorization: ChatAuthorization? = null,
    val userName: ChatUserName? = null,
    val customerId: String? = null,
)

/**
 * Application implementation of [UserName].
 */
@Serializable
data class ChatUserName(
    override val lastName: String,
    override val firstName: String
) : UserName

/** Convert a UserName to a ChatUserName for storage in ChatSettings. */
val UserName.toChatUserName
    get() = if (this is ChatUserName) {
        this
    } else {
        ChatUserName(lastName = lastName, firstName = firstName)
    }

/**
 * Application implementation of [Authorization].
 */
@Serializable
data class ChatAuthorization(
    override val code: String,
    override val verifier: String
) : Authorization

/** Convert an Authorization to a ChatAuthorization for storage in ChatSettings. */
val Authorization.toChatAuthorization
    get() = if (this is ChatAuthorization) {
        this
    } else {
        ChatAuthorization(code, verifier)
    }
