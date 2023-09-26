/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FieldDefinitionList

internal data class ConfigurationInternal(
    override val hasMultipleThreadsPerEndUser: Boolean,
    override val isProactiveChatEnabled: Boolean,
    override val isAuthorizationEnabled: Boolean,
    internal val preContactSurvey: PreChatSurvey?,
    override val contactCustomFields: FieldDefinitionList,
    override val customerCustomFields: FieldDefinitionList,
) : Configuration {
    override fun toString() = "Configuration(" +
            "hasMultipleThreadsPerEndUser=$hasMultipleThreadsPerEndUser, " +
            "isProactiveChatEnabled=$isProactiveChatEnabled, " +
            "isAuthorizationEnabled=$isAuthorizationEnabled, " +
            "preContactSurvey=$preContactSurvey, " +
            "contactCustomFields=${contactCustomFields.joinToString()}, " +
            "customerCustomFields=${customerCustomFields.joinToString()}" +
            ")"
}
