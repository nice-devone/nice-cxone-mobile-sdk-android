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

package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.AgentInternal
import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal class AgentCopyable(
    private val agent: Agent,
) {

    @Suppress("LongParameterList")
    fun copy(
        id: Int = agent.id,
        inContactId: UUID? = agent.inContactId,
        emailAddress: String? = agent.emailAddress,
        firstName: String = agent.firstName,
        lastName: String = agent.lastName,
        nickname: String? = agent.nickname,
        isBotUser: Boolean = agent.isBotUser,
        isSurveyUser: Boolean = agent.isSurveyUser,
        imageUrl: String = agent.imageUrl,
        isTyping: Boolean = agent.isTyping,
    ) = AgentInternal(
        id = id,
        inContactId = inContactId,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = lastName,
        nickname = nickname,
        isBotUser = isBotUser,
        isSurveyUser = isSurveyUser,
        imageUrl = imageUrl,
        isTyping = isTyping
    )

    companion object {

        fun Agent.asCopyable() =
            AgentCopyable(this)
    }
}
