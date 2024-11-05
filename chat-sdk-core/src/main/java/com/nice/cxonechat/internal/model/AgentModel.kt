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

import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AgentModel(
    @SerialName("id")
    val id: Int,

    @SerialName("firstName")
    val firstName: String,

    @SerialName("surname")
    val surname: String,

    @SerialName("nickname")
    val nickname: String? = null,

    @SerialName("isBotUser")
    val isBotUser: Boolean,

    @SerialName("isSurveyUser")
    val isSurveyUser: Boolean,

    @SerialName("publicImageUrl")
    val imageUrl: String,
) {

    fun toAgent(): Agent = AgentInternal(
        id = id,
        firstName = firstName,
        lastName = surname,
        nickname = nickname,
        isBotUser = isBotUser,
        isSurveyUser = isSurveyUser,
        imageUrl = imageUrl,
        isTyping = false,
    )

    fun toMessageAuthor(): MessageAuthor = MessageAuthorInternal(
        id = id.toString(),
        firstName = firstName,
        lastName = surname,
        imageUrl = imageUrl,
        nickname = nickname,
    )
}
