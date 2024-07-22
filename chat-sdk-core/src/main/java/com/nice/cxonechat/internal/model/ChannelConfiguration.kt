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
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.state.FieldDefinitionImpl
import com.nice.cxonechat.state.FileRestrictions as PublicFileRestrictions
import com.nice.cxonechat.state.FileRestrictions.AllowedFileType as PublicAllowedFileType

internal data class ChannelConfiguration(
    @SerializedName("settings")
    val settings: Settings,

    @SerializedName("isAuthorizationEnabled")
    val isAuthorizationEnabled: Boolean,

    @SerializedName("preContactForm")
    val preContactForm: PreContactFormModel?,

    @SerializedName("caseCustomFields")
    val contactCustomFields: List<CustomFieldPolyType>?,

    @SerializedName("endUserCustomFields")
    val customerCustomFields: List<CustomFieldPolyType>?,

    @SerializedName("isLiveChat")
    val isLiveChat: Boolean,

    @SerializedName("availability")
    val availability: Availability,
) {
    data class Settings(
        @SerializedName("hasMultipleThreadsPerEndUser")
        val hasMultipleThreadsPerEndUser: Boolean,

        @SerializedName("isProactiveChatEnabled")
        val isProactiveChatEnabled: Boolean,

        @SerializedName("fileRestrictions")
        val fileRestrictions: FileRestrictions,

        @SerializedName("features")
        val features: Map<String, Boolean>,
    )

    data class FileRestrictions(
        @SerializedName("allowedFileSize")
        val allowedFileSize: Int,

        @SerializedName("allowedFileTypes")
        val allowedFileTypes: List<AllowedFileType>,

        @SerializedName("isAttachmentsEnabled")
        val isAttachmentsEnabled: Boolean,
    )

    data class AllowedFileType(
        @SerializedName("mimeType")
        val mimeType: String,

        @SerializedName("description")
        val description: String,
    )

    data class Availability(
        @SerializedName("status")
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
