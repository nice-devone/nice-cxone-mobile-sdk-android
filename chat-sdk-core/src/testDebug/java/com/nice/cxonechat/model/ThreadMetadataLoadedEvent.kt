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

package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.tool.nextString
import java.util.UUID
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt

@Suppress("LongParameterList")
internal fun makeAgent(
    id: Int = nextInt(),
    inContactId: UUID? = UUID.randomUUID(),
    emailAddress: String? = nextString(),
    firstName: String = nextString(),
    surname: String = nextString(),
    nickname: String? = nextString(),
    isBotUser: Boolean = nextBoolean(),
    isSurveyUser: Boolean = nextBoolean(),
    imageUrl: String = nextString(),
) = AgentModel(
    id = id,
    inContactId = inContactId,
    emailAddress = emailAddress,
    firstName = firstName,
    surname = surname,
    nickname = nickname,
    isBotUser = isBotUser,
    isSurveyUser = isSurveyUser,
    imageUrl = imageUrl
)
