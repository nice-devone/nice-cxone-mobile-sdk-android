package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

internal data class ChannelConfiguration(
    @SerializedName("settings")
    val settings: Settings,

    @SerializedName("isAuthorizationEnabled")
    val isAuthorizationEnabled: Boolean,
) {

    data class Settings(
        @SerializedName("hasMultipleThreadsPerEndUser")
        val hasMultipleThreadsPerEndUser: Boolean,

        @SerializedName("isProactiveChatEnabled")
        val isProactiveChatEnabled: Boolean,
    )

    fun toConfiguration() = ConfigurationInternal(
        hasMultipleThreadsPerEndUser = settings.hasMultipleThreadsPerEndUser,
        isProactiveChatEnabled = settings.isProactiveChatEnabled,
        isAuthorizationEnabled = isAuthorizationEnabled
    )

}
