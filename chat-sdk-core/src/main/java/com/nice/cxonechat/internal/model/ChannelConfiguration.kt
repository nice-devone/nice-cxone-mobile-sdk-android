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

import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.state.FieldDefinitionImpl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.nice.cxonechat.state.FileRestrictions as PublicFileRestrictions
import com.nice.cxonechat.state.FileRestrictions.AllowedFileType as PublicAllowedFileType

@Serializable
internal data class ChannelConfiguration(
    @SerialName("settings")
    val settings: Settings,

    @SerialName("isAuthorizationEnabled")
    val isAuthorizationEnabled: Boolean,

    @SerialName("preContactForm")
    val preContactForm: PreContactFormModel?,

    @SerialName("caseCustomFields")
    val contactCustomFields: List<CustomFieldPolyType>?,

    @SerialName("endUserCustomFields")
    val customerCustomFields: List<CustomFieldPolyType>?,

    @SerialName("isLiveChat")
    val isLiveChat: Boolean,

    @SerialName("availability")
    val availability: Availability,
) {
    @Serializable
    data class Settings(
        @SerialName("hasMultipleThreadsPerEndUser")
        val hasMultipleThreadsPerEndUser: Boolean,

        @SerialName("isProactiveChatEnabled")
        val isProactiveChatEnabled: Boolean,

        @SerialName("fileRestrictions")
        val fileRestrictions: FileRestrictions,

        @SerialName("features")
        val features: Map<String, Boolean>,
    )

    @Serializable
    data class FileRestrictions(
        @SerialName("allowedFileSize")
        val allowedFileSize: Int,

        @SerialName("allowedFileTypes")
        val allowedFileTypes: List<AllowedFileType>,

        @SerialName("isAttachmentsEnabled")
        val isAttachmentsEnabled: Boolean,
    )

    @Serializable
    data class AllowedFileType(
        @SerialName("mimeType")
        val mimeType: String,

        @SerialName("description")
        val description: String,
    )

    @Serializable
    data class Availability(
        @SerialName("status")
        val status: AvailabilityStatus,
    )

    fun toConfiguration(channelId: String) = ConfigurationInternal(
        hasMultipleThreadsPerEndUser = settings.hasMultipleThreadsPerEndUser,
        isProactiveChatEnabled = settings.isProactiveChatEnabled,
        isAuthorizationEnabled = isAuthorizationEnabled,
        preContactSurvey = preContactForm?.toPreContactSurvey(channelId),
        contactCustomFields = contactCustomFields.orEmpty().mapNotNull(FieldDefinitionImpl::invoke).asSequence(),
        customerCustomFields = customerCustomFields.orEmpty().mapNotNull(FieldDefinitionImpl::invoke).asSequence(),
        fileRestrictions = settings.fileRestrictions.toPublic(),
        isLiveChat = isLiveChat,
        isOnline = availability.status == Online,
        features = settings.features,
    )

    companion object {
        private fun FileRestrictions.toPublic() = object : PublicFileRestrictions {
            override val allowedFileSize = this@toPublic.allowedFileSize
            override val allowedFileTypes = this@toPublic.allowedFileTypes
                .filter { allowedFileType ->
                    val mimeTypeParts = allowedFileType.mimeType.split("/")
                    mimeTypeParts.size == 2 && mimeTypeParts.none { part -> part.isEmpty() }
                }
                .map {
                    it.toPublic()
                }
            override val isAttachmentsEnabled = this@toPublic.isAttachmentsEnabled
        }

        private fun AllowedFileType.toPublic() = object : PublicAllowedFileType {
            override val mimeType = this@toPublic.mimeType
            override val description = this@toPublic.description
        }
    }
}
