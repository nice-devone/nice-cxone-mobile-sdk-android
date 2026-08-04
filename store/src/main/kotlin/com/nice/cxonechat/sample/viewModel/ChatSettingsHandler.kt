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

package com.nice.cxonechat.sample.viewModel

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatInstanceProvider.DeviceTokenProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.data.models.ChatSettings
import com.nice.cxonechat.sample.data.models.LoginData
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.toChatAuthorization
import com.nice.cxonechat.sample.data.models.toChatUserName
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository

/**
 * Coordinate chat settings changes between the ChatSettingsRepository and the ChatInstanceProvider.
 *
 * @param context Android context for chat updates.
 * @param chatProvider ChatInstanceProvider to manage.
 * @param chatSettingsRepository ChatSettingsRepository to manage.
 * @param logger [Logger] used as base for [LoggerScope].
 */
class ChatSettingsHandler(
    private val context: Context,
    private val chatProvider: ChatInstanceProvider,
    private val chatSettingsRepository: ChatSettingsRepository,
    logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {
    private val flow by lazy { chatSettingsRepository.settings }

    private val settings: ChatSettings?
        get() = flow.value

    /**
     * Set the sdk configuration to use for future attempts, if the configuration
     * has changed, a new connection will be established.
     *
     * @param sdkConfiguration new configuration to use.
     */
    suspend fun setConfiguration(sdkConfiguration: SdkConfiguration) {
        apply(
            settings?.copy(
                sdkConfiguration = sdkConfiguration,
                authorization = null,
                userName = null,
                connectionToken = null,
            ) ?: ChatSettings(sdkConfiguration, null, null)
        )
    }

    /**
     * Set the login data for future connections.
     *
     * @param loginData New login data to use.
     */
    suspend fun setLoginData(loginData: LoginData) {
        val currentCustomerId = settings?.customerId
        val chatUserName = loginData.userName.toChatUserName
        val customerId = loginData.customerId
        if (currentCustomerId != customerId) {
            apply(
                settings = settings?.copy(userName = chatUserName, customerId = customerId)
            )
        } else {
            settings
                ?.copy(userName = chatUserName)
                ?.let(chatSettingsRepository::use)
                ?: chatSettingsRepository.clear()
            chatProvider.setUserName(chatUserName)
        }
    }

    /**
     * Set the authorization to use for future connections.
     *
     * A new connection will be established
     *
     * @param authorization new authorization to use.
     * @param onPersisted Optional callback invoked after the authorization has been persisted.
     */
    suspend fun setAuthorization(
        authorization: Authorization,
        onPersisted: (() -> Unit)? = null,
    ) {
        apply(
            settings = settings?.copy(
                authorization = authorization.toChatAuthorization,
            ),
            onPersisted = onPersisted
        )
    }

    /**
     * Update the JWT connection token and its expiry without disrupting an active session.
     * Persists both values so the delegate can reconstruct an [com.nice.cxonechat.OAuthToken]
     * on the next call.
     *
     * @param token JWT token to update, or null to clear.
     * @param expiryMillis Unix epoch millis of token expiry, or null to clear.
     */
    fun updateConnectionTokenOnly(token: String?, expiryMillis: Long? = null) = scope("updateConnectionTokenOnly") {
        val updatedSettings = settings?.copy(connectionToken = token, connectionTokenExpiry = expiryMillis) ?: return@scope
        chatSettingsRepository.use(updatedSettings)
    }

    /**
     * Return the cached JWT from persisted settings, or null if none is stored.
     */
    fun getCachedConnectionToken(): String? = settings?.connectionToken

    /**
     * Return the cached token expiry from persisted settings, or null if none is stored.
     */
    fun getCachedConnectionTokenExpiry(): Long? = settings?.connectionTokenExpiry

    /**
     * Persist the JSON-serialized AppAuth [net.openid.appauth.AuthState] so that
     * [net.openid.appauth.AuthState.performActionWithFreshTokens] can be used for silent
     * token refresh without prompting the user.
     *
     * **Note:** The [AuthState] JSON (which may include a refresh token) is stored in plain
     * internal storage via [chatSettingsRepository]. This is intentional for demo and debugging
     * purposes only — do **not** use unencrypted storage for refresh tokens in a production app.
     * Consider using [androidx.security.crypto.EncryptedFile] or a dedicated keystore-backed
     * solution to protect sensitive credentials.
     */
    fun saveAuthStateJson(json: String?) = scope("saveAuthStateJson") {
        val updatedSettings = settings?.copy(authStateJson = json) ?: return@scope
        chatSettingsRepository.use(updatedSettings)
    }

    /**
     * Return the JSON-serialized AppAuth [net.openid.appauth.AuthState], or null if not stored.
     */
    fun getAuthStateJson(): String? = settings?.authStateJson

    /**
     * Clear any saved user authentication credentials from the ChatProvider
     * and saved storage.
     * AppAuth is stateless — no persistent token store to clear on the library side.
     */
    suspend fun clearAuthentication() = scope("clearAuthentication") {
        apply(
            settings?.copy(authorization = null, userName = null, customerId = null, connectionToken = null, authStateJson = null)
        )
    }

    /**
     * Save the settings changes and apply them to the chatProvider.
     *
     * @param settings ChatSettings to apply, if null is supplied the settings will be cleared and chatProvider will be signed out.
     * @param onPersisted Optional callback invoked after the settings have been persisted,
     * after the chatProvider has been updated with the new settings. Invoked only if settings is not null.
     */
    private suspend fun apply(settings: ChatSettings?, onPersisted: (() -> Unit)? = null) = scope("apply") {
        if (settings == null) {
            chatSettingsRepository.clear()
            chatProvider.signOut()
        } else {
            chatSettingsRepository.use(settings)
            chatProvider.configure(context) {
                configuration = settings.sdkConfiguration?.asSocketFactoryConfiguration
                userName = settings.userName
                authorization = settings.authorization
                customerId = settings.customerId
                deviceTokenProvider = FirebaseTokenProvider()
            }
            onPersisted?.invoke()
        }
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private const val TAG = "ChatSettingsHandler"
    }

    private inner class FirebaseTokenProvider :
        DeviceTokenProvider,
        LoggerScope by LoggerScope<DeviceTokenProvider>(this) {
        override fun requestDeviceToken(onComplete: (String) -> Unit): Unit =
            scope("requestDeviceToken") {
                Firebase
                    .messaging
                    .token
                    .addOnSuccessListener(onComplete)
                    .addOnFailureListener {
                        error("Firebase.messaging.token failed: $it")
                    }
            }
    }
}
