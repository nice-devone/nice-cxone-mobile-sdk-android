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

package com.nice.cxonechat.sample.viewModel

import android.content.Context
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatInstanceProvider.DeviceTokenProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.info
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
    private val settings: ChatSettings?
        get() = chatSettingsRepository.settings.value

    /**
     * Set the sdk configuration to use for future attempts, if the configuration
     * has changed, a new connection will be established.
     *
     * @param sdkConfiguration new configuration to use.
     */
    fun setConfiguration(sdkConfiguration: SdkConfiguration) {
        apply(
            settings?.copy(
                sdkConfiguration = sdkConfiguration,
                authorization = null,
                userName = null,
            ) ?: ChatSettings(sdkConfiguration, null, null)
        )
    }

    /**
     * Set the login data for future connections.
     *
     * @param loginData New login data to use.
     */
    fun setLoginData(loginData: LoginData) {
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
     */
    fun setAuthorization(authorization: Authorization) {
        apply(
            settings?.copy(
                authorization = authorization.toChatAuthorization,
            )
        )
    }

    /**
     * Clear any saved user authentication credentials from the ChatProvider
     * and saved storage.
     */
    fun clearAuthentication() = scope("clearAuthentication") {
        AuthorizationManager.signOut(context, LoggingSignoutListener())

        apply(
            settings?.copy(authorization = null, userName = null, customerId = null)
        )
    }

    /**
     * Apply save a set of settings changes and apply them to the chatProvider.
     *
     * @param settings ChatSettings to apply.
     */
    private fun apply(settings: ChatSettings?) = scope("apply") {
        settings?.let(chatSettingsRepository::use) ?: chatSettingsRepository.clear()

        chatProvider.signOut()

        chatProvider.configure(context) {
            configuration = settings?.sdkConfiguration?.asSocketFactoryConfiguration
            userName = settings?.userName
            authorization = settings?.authorization
            customerId = settings?.customerId
            deviceTokenProvider = FirebaseTokenProvider()
        }
    }

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

    private inner class LoggingSignoutListener :
        Listener<Void, AuthError>,
        LoggerScope by LoggerScope<LoggingSignoutListener>(this) {
        override fun onSuccess(ignore: Void?) = scope("onSuccess") {
            info("loginWithAmazon.logout success")
        }

        override fun onError(error: AuthError?) = scope("onError") {
            info("loginWithAmazon.logout failure: ${error?.message}")
        }
    }
}
