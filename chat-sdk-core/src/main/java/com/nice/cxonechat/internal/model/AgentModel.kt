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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal data class AgentModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("inContactId")
    val inContactId: UUID?,

    @SerializedName("emailAddress")
    val emailAddress: String?,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("surname")
    val surname: String,

    @SerializedName("nickname")
    val nickname: String?,

    @SerializedName("isBotUser")
    val isBotUser: Boolean,

    @SerializedName("isSurveyUser")
    val isSurveyUser: Boolean,

    @SerializedName("imageUrl")
    val imageUrl: String,
) {

    fun toAgent(): Agent = AgentInternal(
        id = id,
        inContactId = inContactId,
        emailAddress = emailAddress,
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
    )
}
