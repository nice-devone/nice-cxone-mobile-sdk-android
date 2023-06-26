package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyInternal
import com.nice.cxonechat.state.FieldDefinitionImpl

internal data class PreContactFormModel(
    @SerializedName("name")
    val name: String,

    @SerializedName("channels")
    val channels: List<ChannelIdentifier>,

    @SerializedName("customFields")
    val customFields: List<PreContactCustomFieldDefinitionModel>,
) {

    fun toPreContactSurvey(currentChannel: String): PreChatSurvey? {
        val currentId = ChannelIdentifier(currentChannel)
        val modelList = if (!channels.contains(currentId)) emptyList() else customFields
        val surveyTypes = modelList
            .mapNotNull(FieldDefinitionImpl::invoke)
            .ifEmpty {
                return null
            }
        return PreChatSurveyInternal(
            name = name,
            fields = surveyTypes.asSequence()
        )
    }
}
