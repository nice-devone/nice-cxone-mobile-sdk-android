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
) : Configuration() {
    override fun toString() = "Configuration(" +
            "hasMultipleThreadsPerEndUser=$hasMultipleThreadsPerEndUser, " +
            "isProactiveChatEnabled=$isProactiveChatEnabled, " +
            "isAuthorizationEnabled=$isAuthorizationEnabled, " +
            "preContactSurvey=$preContactSurvey, " +
            "contactCustomFields=$contactCustomFields, " +
            "customerCustomFields=$customerCustomFields" +
            ")"
}
