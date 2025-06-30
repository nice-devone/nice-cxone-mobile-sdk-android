/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.data.models.ChatSettings
import com.nice.cxonechat.sample.data.models.LoginData
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.ExtraCustomFieldRepository
import com.nice.cxonechat.sample.data.repository.SdkConfigurationListRepository
import com.nice.cxonechat.sample.data.repository.StoreRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.extensions.Ignored
import com.nice.cxonechat.sample.viewModel.UiState.Configuration
import com.nice.cxonechat.sample.viewModel.UiState.Initial
import com.nice.cxonechat.sample.viewModel.UiState.Login
import com.nice.cxonechat.sample.viewModel.UiState.OAuth
import com.nice.cxonechat.sample.viewModel.UiState.Prepared
import com.nice.cxonechat.sample.viewModel.UiState.Preparing
import com.nice.cxonechat.sample.viewModel.UiState.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the StoreActivity.
 */
@Stable
@Suppress("LongParameterList")
@KoinViewModel
class StoreViewModel(
    application: Application,
    /** store repository containing cart and product information. */
    val storeRepository: StoreRepository,
    /** sdk configuration repository containing list of predefined SDK configurations. */
    val sdkConfigurationListRepository: SdkConfigurationListRepository,
    /** chat configuration and settings repository containing the current configuration. */
    val chatSettingsRepository: ChatSettingsRepository,
    /** UI settings repository saving and managing UI configuration. */
    val uiSettingsRepository: UISettingsRepository,
    /** Persistence for extra custom fields. */
    val extraCustomFieldRepository: ExtraCustomFieldRepository,
    /** chat repository containing current chat. */
    val chatProvider: ChatInstanceProvider,
    /** logger for store messages. */
    logger: Logger,
) : AndroidViewModel(application), LoggerScope by LoggerScope(TAG, logger) {
    private val uiStateStore = MutableStateFlow<UiState>(Initial)

    private val context
        get() = getApplication() as Context

    /** Page view handler managing page view and page view ended events. */
    val analyticsHandler = AnalyticsHandler(chatProvider, this)

    /** Chat settings handler to manage settings updates into provider. */
    val chatSettingsHandler = ChatSettingsHandler(context, chatProvider, chatSettingsRepository, this)

    /** Current UI State. */
    @Stable
    val uiState = uiStateStore.asStateFlow()

    /** listener to chat instance changes. */
    private val listener = Listener().also(chatProvider::addListener)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (chatSettingsRepository.load() == null) setUiState(Configuration) else chatProvider.prepare(context)
            uiSettingsRepository.load()
            sdkConfigurationListRepository.load()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatProvider.removeListener(listener)
    }

    /**
     * Update UI State.
     *
     * @param state new UI State.
     */
    fun setUiState(state: UiState) = scope("setUiState") {
        debug("uiState: ${uiState.value} -> $state")
        uiStateStore.value = state
    }

    /**
     * Start up chat as required/possible.
     */
    private fun startChat() {
        listener.onChatStateChanged(chatProvider.chatState)
    }

    /**
     * present SDK configuration alert.
     */
    fun presentConfigurationDialog() {
        setUiState(Configuration)
    }

    /** Display the UI Settings dialog. */
    fun presentUiSettings() {
        setUiState(UiSettings)
    }

    /**
     * Set the login data for future connections.
     *
     * @param loginData New login data to use.
     */
    fun setLoginData(loginData: LoginData) {
        chatSettingsHandler.setLoginData(loginData)

        if (chatProvider.chatState == ChatState.Initial) {
            chatProvider.prepare(context)
        } else {
            listener.onChatStateChanged(chatProvider.chatState)
        }
    }

    /**
     * A connection has been established, check it's validity based on:
     *  * if authentication is enabled, make sure we have appropriate OAuth details
     *  * otherwise make sure we have a valid user name.
     */
    private fun onConnected() = scope("onConnected") {
        val settings = chatSettingsRepository.settings.value
        val isAuthorizationEnabled = chatProvider.chat?.configuration?.isAuthorizationEnabled
        val state = currentUiState(this, settings, isAuthorizationEnabled) ?: return@scope
        val currentState = uiState.value
        if (!(state == Prepared && (currentState is UiSettings || currentState is Configuration))) {
            setUiState(state)
        }
    }

    private fun currentUiState(
        loggerScope: LoggerScope,
        settings: ChatSettings?,
        isAuthorizationEnabled: Boolean?,
    ) = when (isAuthorizationEnabled) {
        null -> {
            loggerScope.error("No chat when in CONNECTED state.")
            null
        }

        true -> if (settings?.authorization != null) {
            Prepared
        } else {
            OAuth
        }

        false -> if (settings?.userName != null) {
            Prepared
        } else {
            Login
        }
    }

    /**
     * Log out, clearing out all configuration-dependent information.
     *
     * Clears and Resets:
     * * Chat connection
     * * UI Settings
     * * Store cart and user information
     */
    fun logout() {
        chatSettingsHandler.clearAuthentication()
        extraCustomFieldRepository.clear(context)
        // This needs to be *after* all the settings are cleared out or we
        // immediately reconnect using the same information.
        chatProvider.signOut()
    }

    /**
     * Called by hosting [android.app.Activity.onResume] to generate appropriate page view events when
     * the application is resumed and to restart the chat sdk and enable analytics.
     */
    fun onResume() {
        startChat()
    }

    private inner class Listener: ChatInstanceProvider.Listener {
        /**
         * Send a pending page view if chat is just now established.
         */
        override fun onChatStateChanged(chatState: ChatState) {
            // If the chat has now connected, see if we need to send authorization
            when (chatState) {
                ChatState.Initial -> setUiState(Configuration)
                ChatState.Preparing -> setUiState(Preparing)
                ChatState.Prepared -> onConnected()
                else -> Ignored
            }
        }

        override fun onChatRuntimeException(exception: RuntimeChatException) = scope("onChatRuntimeException") {
            error("Chat SDK reported exception.", exception)
        }
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private const val TAG = "StoreViewModel"
    }
}
