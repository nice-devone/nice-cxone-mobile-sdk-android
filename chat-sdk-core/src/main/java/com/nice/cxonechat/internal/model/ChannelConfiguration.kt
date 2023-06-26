package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.state.FieldDefinitionImpl

internal data class ChannelConfiguration(
    @SerializedName("settings")
    val settings: Settings,

    @SerializedName("isAuthorizationEnabled")
    val isAuthorizationEnabled: Boolean,

    @SerializedName("preContactForm")
    val preContactForm: PreContactFormModel?,

    @SerializedName("caseCustomFields")
    val contactCustomFields: List<CustomFieldPolyType>,

    @SerializedName("endUserCustomFields")
    val customerCustomFields: List<CustomFieldPolyType>,
) {
    data class Settings(
        @SerializedName("hasMultipleThreadsPerEndUser")
        val hasMultipleThreadsPerEndUser: Boolean,

        @SerializedName("isProactiveChatEnabled")
        val isProactiveChatEnabled: Boolean,
    )

    fun toConfiguration(channelId: String) = ConfigurationInternal(
        hasMultipleThreadsPerEndUser = settings.hasMultipleThreadsPerEndUser,
        isProactiveChatEnabled = settings.isProactiveChatEnabled,
        isAuthorizationEnabled = isAuthorizationEnabled,
        preContactSurvey = preContactForm?.toPreContactSurvey(channelId),
        contactCustomFields = contactCustomFields.mapNotNull(FieldDefinitionImpl::invoke).asSequence(),
        customerCustomFields = customerCustomFields.mapNotNull(FieldDefinitionImpl::invoke).asSequence(),
    )
}
