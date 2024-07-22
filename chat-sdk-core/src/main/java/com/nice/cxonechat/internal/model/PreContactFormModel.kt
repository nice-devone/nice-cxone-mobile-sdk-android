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
