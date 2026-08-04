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

package com.nice.cxonechat.sample.utilities

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.nice.cxonechat.sample.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.AuthorizationServiceConfiguration

/**
 * Reads OAuth configuration from [R.raw.auth_config] and exposes typed properties
 * for use with the AppAuth library.
 */
internal class AuthConfiguration private constructor(raw: RawConfig) {

    val androidClientId: String = raw.androidClientId
    val webClientId: String = raw.webClientId
    val redirectUri: Uri = raw.redirectUri.toUri()
    val scope: String = raw.authorizationScope
    val serviceConfiguration: AuthorizationServiceConfiguration = AuthorizationServiceConfiguration(
        raw.authorizationEndpointUri.toUri(),
        raw.tokenEndpointUri.toUri(),
    )

    @Serializable
    private data class RawConfig(
        @SerialName("android_client_id") val androidClientId: String,
        @SerialName("web_client_id") val webClientId: String,
        @SerialName("redirect_uri") val redirectUri: String,
        @SerialName("authorization_endpoint_uri") val authorizationEndpointUri: String,
        @SerialName("token_endpoint_uri") val tokenEndpointUri: String,
        @SerialName("authorization_scope") val authorizationScope: String,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun from(context: Context): AuthConfiguration {
            val raw = context.resources.openRawResource(R.raw.auth_config)
                .bufferedReader()
                .use { it.readText() }
            return AuthConfiguration(json.decodeFromString(raw))
        }
    }
}
