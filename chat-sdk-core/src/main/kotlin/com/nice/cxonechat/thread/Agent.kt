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

package com.nice.cxonechat.thread

import com.nice.cxonechat.Public

/**
 * Represents all info about an agent.
 */
@Public
abstract class Agent {
    /** The id of the agent. */
    abstract val id: Int

    /** The first name of the agent, or `null` when not provided by the backend (e.g. when the agent's personal information is hidden). */
    abstract val firstName: String?

    /** The surname of the agent, or `null` when not provided by the backend (e.g. when the agent's personal information is hidden). */
    abstract val lastName: String?

    /**
     * The optional nickname of the agent.
     *
     * A `null` value means the agent has no nickname assigned; this field is not affected by agent PII hiding.
     */
    abstract val nickname: String?

    /**
     * Whether the agent is a bot, or `null` when the agent's bot status is not known.
     *
     * A `null` value should be treated as "unknown" — the backend did not provide this information.
     * Custom UI implementations should not assert that the agent is/is not a bot when this is `null`.
     */
    abstract val isBotUser: Boolean?

    /**
     * Whether the agent is for automated surveys, or `null` when the agent's survey status is not known.
     *
     * A `null` value should be treated as "unknown" — the backend did not provide this information.
     */
    abstract val isSurveyUser: Boolean?

    /**
     * The URL for the profile photo of the agent, or `null` when not provided by the backend
     * (e.g. when the agent's personal information is hidden).
     */
    abstract val imageUrl: String?

    /** Indicates that agent is currently typing. */
    abstract val isTyping: Boolean

    /** The full name of the agent, or `null` when neither first name nor last name was provided by the backend, or both are blank. */
    val fullName: String?
        get() = listOfNotNull(firstName?.ifBlank { null }, lastName?.ifBlank { null })
            .joinToString(" ")
            .ifBlank { null }
}
