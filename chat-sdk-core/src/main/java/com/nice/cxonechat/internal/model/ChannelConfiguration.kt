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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
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

    @SerialName("isSecuredCookieEnabled")
    val isSecuredCookieEnabled: Boolean,

    @SerialName("preContactForm")
    val preContactForm: PreContactFormModel?,

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

        @SerialName("liveChatAllowTranscript")
        val liveChatAllowTranscript: Boolean,

        @SerialName("securedSessions")
        val securedSessions: Boolean,

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
        isSecuredCookieEnabled = isSecuredCookieEnabled,
        securedSessions = settings.securedSessions,
        authenticationType = determineAuthenticationType(),
        preContactSurvey = preContactForm?.toPreContactSurvey(channelId),
        fileRestrictions = settings.fileRestrictions.toPublic(),
        liveChatAllowTranscript = settings.liveChatAllowTranscript,
        isLiveChat = isLiveChat,
        isOnline = availability.status == Online,
        features = settings.features,
    )

    /**
     * Determines the authentication type based on channel configuration.
     *
     * **Authentication Type Priority Order:**
     * 1. [AuthenticationType.SecuredCookie] - if [isSecuredCookieEnabled] is true
     * 2. [AuthenticationType.ThirdPartyOAuth] - if [isAuthorizationEnabled] is true
     * 3. [AuthenticationType.Anonymous] - default fallback
     *
     * **IMPORTANT**: If both [isSecuredCookieEnabled] and [isAuthorizationEnabled] are true,
     * the backend configuration may be ambiguous. In this case, [AuthenticationType.SecuredCookie]
     * takes precedence. This priority should be validated against backend configuration to ensure
     * it matches the intended authentication behavior.
     *
     * This priority is by design to ensure secured cookie authentication (server-side sessions)
     * takes precedence over OAuth when both are enabled, as it provides a fallback mechanism.
     *
     * @return The determined [AuthenticationType] for this channel
     */
    private fun determineAuthenticationType(): AuthenticationType = when {
        isSecuredCookieEnabled -> AuthenticationType.SecuredCookie
        isAuthorizationEnabled -> AuthenticationType.ThirdPartyOAuth
        else -> AuthenticationType.Anonymous
    }

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

            override fun toString(): String = buildString {
                append("FileRestrictions(")
                append("allowedFileSize=$allowedFileSize")
                append(", allowedFileTypes=${allowedFileTypes.joinToString()}")
                append(", isAttachmentsEnabled=$isAttachmentsEnabled")
                append(")")
            }
        }

        private fun AllowedFileType.toPublic() = object : PublicAllowedFileType {
            override val mimeType = this@toPublic.mimeType
            override val description = this@toPublic.description
            override fun toString(): String = "AllowedFileType(mimeType='$mimeType', description='$description')"
        }
    }
}
