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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AgentModel(
    @SerialName("id")
    val id: Int,

    @SerialName("firstName")
    val firstName: String? = null,

    @SerialName("surname")
    val surname: String? = null,

    @SerialName("nickname")
    val nickname: String? = null,

    @SerialName("isBotUser")
    val isBotUser: Boolean? = null,

    @SerialName("isSurveyUser")
    val isSurveyUser: Boolean? = null,

    /**
     * The value is non-null unless ANY PII filtering is applied on backend, in such case use [filteredFallbackImageUrl] instead.
     */
    @SerialName("publicImageUrl")
    val imageUrl: String? = null,

    /**
     * The value can be used only iff non-image PII filtering is applied on backend.
     */
    @SerialName("imageUrl")
    val filteredFallbackImageUrl: String? = null,
) {

    fun toAgent(): Agent = AgentInternal(
        id = id,
        firstName = firstName,
        lastName = surname,
        nickname = nickname,
        isBotUser = isBotUser,
        isSurveyUser = isSurveyUser,
        imageUrl = imageUrl ?: filteredFallbackImageUrl,
        isTyping = false,
    )

    fun toMessageAuthor(): MessageAuthor = MessageAuthorInternal(
        id = id.toString(),
        firstName = firstName.orEmpty(),
        lastName = surname.orEmpty(),
        imageUrl = imageUrl ?: filteredFallbackImageUrl,
        nickname = nickname,
    )
}
