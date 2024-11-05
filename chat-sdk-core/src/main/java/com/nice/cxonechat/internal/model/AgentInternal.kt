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

import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal data class AgentInternal(
    override val id: Int,
    @Deprecated("inContactId is internal field and should not be used. It is now always null.")
    override val inContactId: UUID? = null,
    @Deprecated("emailAddress is internal field and should not be used. It is now always null.")
    override val emailAddress: String? = null,
    override val firstName: String,
    override val lastName: String,
    override val nickname: String?,
    override val isBotUser: Boolean,
    override val isSurveyUser: Boolean,
    override val imageUrl: String,
    override val isTyping: Boolean,
) : Agent() {

    override fun toString() = buildString {
        append("Agent(id=")
        append(id)
        append(", firstName='")
        append(firstName)
        append("', lastName='")
        append(lastName)
        append("', nickname=")
        append(nickname)
        append(", isBotUser=")
        append(isBotUser)
        append(", isSurveyUser=")
        append(isSurveyUser)
        append(", imageUrl='")
        append(imageUrl)
        append("', isTyping=")
        append(isTyping)
        append(")")
    }
}
