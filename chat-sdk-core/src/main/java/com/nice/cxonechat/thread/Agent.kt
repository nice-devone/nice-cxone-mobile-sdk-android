/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.thread

import com.nice.cxonechat.Public

/**
 * Represents all info about an agent.
 */
@Public
abstract class Agent {
    /** The id of the agent. */
    abstract val id: Int

    /** The first name of the agent. */
    abstract val firstName: String

    /** The surname of the agent. */
    abstract val lastName: String

    /** The optional nickname of the agent. */
    abstract val nickname: String?

    /** Whether the agent is a bot. */
    abstract val isBotUser: Boolean

    /** Whether the agent is for automated surveys. */
    abstract val isSurveyUser: Boolean

    /** The URL for the profile photo of the agent. */
    abstract val imageUrl: String

    /** Indicates that agent is currently typing. */
    abstract val isTyping: Boolean

    /** The full name of the agent (readonly). */
    val fullName: String
        get() = "$firstName $lastName".trim()
}
