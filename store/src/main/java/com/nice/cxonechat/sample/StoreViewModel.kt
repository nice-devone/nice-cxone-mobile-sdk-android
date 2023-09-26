/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatEventHandlerActions.conversion
import com.nice.cxonechat.ChatEventHandlerActions.pageView
import com.nice.cxonechat.ChatEventHandlerActions.pageViewEnded
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatInstanceProvider.DeviceTokenProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.UserName
import com.nice.cxonechat.sample.UiState.CONFIGURATION
import com.nice.cxonechat.sample.UiState.CONNECTED
import com.nice.cxonechat.sample.UiState.CONNECTING
import com.nice.cxonechat.sample.UiState.LOGIN
import com.nice.cxonechat.sample.UiState.OAUTH
import com.nice.cxonechat.sample.UiState.UI_SETTINGS
import com.nice.cxonechat.sample.data.models.ChatSettings
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.toChatAuthorization
import com.nice.cxonechat.sample.data.models.toChatUserName
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.SdkConfigurationListRepository
import com.nice.cxonechat.sample.data.repository.StoreRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.extensions.Ignored
import com.nice.cxonechat.sample.extensions.OnLifecycleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import javax.inject.Inject

/** Current state of the UI. */
enum class UiState {
    /** Nothing has been done yet. */
    INITIAL,

    /** Requesting Configuration details from the user. */
    CONFIGURATION,

    /** Attempting to connect. */
    CONNECTING,

    /** Performing OAuth authentication with the user. */
    OAUTH,

    /** Performing simple authentication with the user. */
    LOGIN,

    /** Displaying the UI Settings dialog. */
    UI_SETTINGS,

    /** connected to the server. */
    CONNECTED
}

/**
 * ViewModel for the StoreActivity.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class StoreViewModel @Inject constructor(
    application: Application,
    /** store repository containing cart and product information. */
    val storeRepository: StoreRepository,
    /** sdk configuration repository containing list of predefined SDK configurations. */
    val sdkConfigurationListRepository: SdkConfigurationListRepository,
    /** chat configuration and settings repository containing the current configuration. */
    val chatSettingsRepository: ChatSettingsRepository,
    /** UI settings repository saving and managing UI configuration. */
    val uiSettingsRepository: UISettingsRepository,
) : AndroidViewModel(application), ChatInstanceProvider.Listener {
    /** chat repository containing current chat. */
    val chatProvider = ChatInstanceProvider.get()

    @Immutable
    private data class PageView(val title: String, val url: String, val date: Date = Date())

    private var currentPageView: PageView? = null
        set(value) {
            Log.v(TAG, "currentPageView = $field -> $value")
            field = value
        }

    private val uiStateStore = MutableStateFlow(UiState.INITIAL)

    private val context
        get() = getApplication() as Context

    /** Current UI State. */
    @Stable
    val uiState = uiStateStore.asStateFlow()

    init {
        chatSettingsRepository.load()
        uiSettingsRepository.load()
        sdkConfigurationListRepository.load()
    }

    /**
     * Update UI State.
     *
     * @param expect expected UI state.  State will only be updated if the current state is [expect].
     * @param state new UI State.
     */
    private fun setUiState(expect: UiState, state: UiState) {
        if (state == uiState.value) {
            Log.d(TAG, "uiState: ${uiState.value} -> $state ignored")
        } else if (uiStateStore.compareAndSet(expect, state)) {
            Log.d(TAG, "uiState: $expect -> $state")
        } else {
            Log.d(TAG, "setUiState: expected state change: $expect -> $state: actual=${uiState.value}")
        }
    }

    /**
     * Update UI State.
     *
     * @param state new UI State.
     */
    private fun setUiState(state: UiState) {
        Log.d(TAG, "uiState: ${uiState.value} -> $state")
        uiStateStore.value = state
    }

    /**
     * Start up chat as required/possible.
     */
    fun startChat() {
        chatProvider.addListener(this)

        if (chatSettingsRepository.settings.value == null) {
            setUiState(CONFIGURATION)
        }
        else if (!setOf(CONNECTED, CONNECTING).contains(uiState.value)) {
            connect()
        }
    }

    /**
     * Stop the chat as required.
     */
    fun stopChat() {
        chatProvider.removeListener(this)
        chatProvider.stop()
    }

    /**
     * present SDK configuration alert.
     */
    fun presentConfigurationDialog() {
        setUiState(CONFIGURATION)
    }

    /**
     * Cancel the sdk configuration dialog.
     */
    fun cancelConfigurationDialog() {
        setUiState(CONFIGURATION, CONNECTED)
    }

    /** Display the UI Settings dialog. */
    fun presentUiSettings() {
        setUiState(UI_SETTINGS)
    }

    /** Cancel the UI Settings dialog. */
    fun cancelUiSettings() {
        setUiState(UI_SETTINGS, CONNECTED)
    }

    /**
     * Set the sdk configuration to use for future attempts, if the configuration
     * has changed, a new connection will be established.
     *
     * @param sdkConfiguration new configuration to use.
     */
    fun setConfiguration(sdkConfiguration: SdkConfiguration) {
        val settings = chatSettingsRepository.settings.value?.copy(
            sdkConfiguration = sdkConfiguration,
            authorization = null,
            userName = null,
        ) ?: ChatSettings(sdkConfiguration, null, null)

        apply(settings)
    }

    /**
     * Set the user name for future connections.
     *
     * If the name changes, a new connection will be established
     *
     * @param userName New user name to use.
     */
    fun setUserName(userName: UserName) {
        apply(
            chatSettingsRepository.settings.value?.copy(
                userName = userName.toChatUserName,
            )
        )
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
            chatSettingsRepository.settings.value?.copy(
                authorization = authorization.toChatAuthorization,
            )
        )
    }

    /**
     * Apply save a set of settings changes and apply them to the chatProvider.
     *
     * @param settings ChatSettings to apply.
     */
    private fun apply(settings: ChatSettings?) {
        settings?.let(chatSettingsRepository::use) ?: chatSettingsRepository.clear()

        chatProvider.signOut()

        chatProvider.configure(context) {
            configuration = settings?.sdkConfiguration?.asSocketFactoryConfiguration
            userName = settings?.userName
            authorization = settings?.authorization
            deviceTokenProvider = DeviceTokenProvider { setToken ->
                Firebase
                    .messaging
                    .token
                    .addOnSuccessListener(setToken)
                    .addOnFailureListener {
                        Log.e(TAG, "Firebase.messaging.token failed: $it")
                    }
            }
        }
    }

    /**
     * A connection has been established, check it's validity based on:
     *  * if authentication is enabled, make sure we have appropriate OAuth details
     *  * otherwise make sure we have a valid user name.
     */
    private fun onConnected() {
        val settings = chatSettingsRepository.settings.value

        when (chatProvider.chat?.configuration?.isAuthorizationEnabled) {
            null -> {
                Log.e(TAG, "No chat when in CONNECTED state.")
                null
            }

            true -> if (settings?.authorization != null) {
                CONNECTED
            } else {
                OAUTH
            }

            false -> if (settings?.userName != null) {
                CONNECTED
            } else {
                LOGIN
            }
        }?.also { state ->
                if (!(setOf(UI_SETTINGS, CONFIGURATION).contains(uiState.value) && state == CONNECTED)) {
                    setUiState(state)
                }
                if (state == CONNECTED) {
                    // If we're connected for real now, send a pending page view
                    sendPageView()
                }
        }
    }

    /**
     * Clear any saved user authentication credentials from the ChatProvider
     * and saved storage.
     */
    private fun clearAuthentication() {
        AuthorizationManager.signOut(
            context,
            object : Listener<Void, AuthError> {
                override fun onSuccess(ignore: Void?) {
                    Log.i(TAG, "loginWithAmazon.logout success")
                }

                override fun onError(error: AuthError?) {
                    Log.i(TAG, "loginWithAmazon.logout failure: ${error?.message}")
                }
            }
        )

        apply(
            chatSettingsRepository.settings.value?.copy(authorization = null, userName = null)
        )
    }

    /**
     * Attempt to connect to the chat server with the info we have available to date.
     *
     * The ui state will be advanced to CONFIGURATION, LOGIN, or OAUTH depending on results.
     * In a "normal" environment, this is all unnecessary as the host application will
     * have a predefined configuration and should be prepared in advance to perform
     * OAuth authentication or collect user information as needed.
     */
    fun connect() {
        chatProvider.start(context)
    }

    /**
     * Cancel a pending [ChatProvider.start] request.
     */
    fun cancelConnect() {
        chatProvider.cancelStart()
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
        clearAuthentication()

        // This needs to be *after* all the settings are cleared out or we
        // immediately reconnect using the same information.
        chatProvider.signOut()
    }

    override fun onCleared() {
        Log.v(TAG, "ViewModel cleared")
        super.onCleared()
    }

    /**
     * Start a LaunchedEffect to send the page view event when possible and
     * necessary.
     *
     * @param title Title of page to send.
     * @param url URL of page to send.
     */
    @Composable
    fun SendPageView(title: String, url: String) {
        currentPageView = PageView(title, url)

        OnLifecycleEvent { _, event ->
            when (event) {
                ON_RESUME -> sendPageView()
                ON_PAUSE -> sendPageViewEnded()
                else -> Ignored
            }
        }
    }

    private fun sendPageView() {
        currentPageView?.run {
            chatProvider.chat?.events()?.pageView(title, url, date)
        }
    }

    private fun sendPageViewEnded() {
        currentPageView?.run {
            chatProvider.chat?.events()?.pageViewEnded(title, url, Date())
        }
    }

    /**
     * Send a conversion event to the analytics service.
     *
     * @param type application-specific "type" of conversion.
     * @param amount dollar amount of conversion.
     * @param date date of conversion, defaults to now.
     */
    fun sendConversion(type: String, amount: Double, date: Date = Date()) {
        chatProvider.chat?.events()?.conversion(type, amount, date)
    }

    //
    // ChatInstanceProvider.Listener Implementation
    //

    /**
     * Send a pending page view if chat is just now established.
     */
    override fun onChatStateChanged(chatState: ChatState) {
        // If the chat has now connected, see if we need to send authorization
        when (chatState) {
            ChatState.INITIAL -> setUiState(CONFIGURATION)
            ChatState.CONNECTING -> setUiState(CONNECTING)
            ChatState.CONNECTED -> onConnected()
            else -> Ignored
        }
    }

    companion object {
        private const val TAG = "StoreViewModel"
    }
}
