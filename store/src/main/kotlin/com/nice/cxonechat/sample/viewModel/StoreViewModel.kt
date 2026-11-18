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

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenRequestReason.TOKEN_EXPIRED
import com.nice.cxonechat.TokenRequestReason.TOKEN_INVALID
import com.nice.cxonechat.TokenRequestReason.UNSPECIFIED
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.sample.data.models.ChatSettings
import com.nice.cxonechat.sample.data.models.LoginData
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.ExtraCustomFieldRepository
import com.nice.cxonechat.sample.data.repository.LaunchEnvironments
import com.nice.cxonechat.sample.data.repository.SdkConfigurationListRepository
import com.nice.cxonechat.sample.data.repository.StoreRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.extensions.Ignored
import com.nice.cxonechat.sample.viewModel.UiState.Configuration
import com.nice.cxonechat.sample.viewModel.UiState.ImplicitOAuth
import com.nice.cxonechat.sample.viewModel.UiState.Initial
import com.nice.cxonechat.sample.viewModel.UiState.Login
import com.nice.cxonechat.sample.viewModel.UiState.OAuth
import com.nice.cxonechat.sample.viewModel.UiState.OpeningChat
import com.nice.cxonechat.sample.viewModel.UiState.Prepared
import com.nice.cxonechat.sample.viewModel.UiState.Preparing
import com.nice.cxonechat.sample.viewModel.UiState.SdkNotSupported
import com.nice.cxonechat.sample.viewModel.UiState.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import org.koin.core.annotation.KoinViewModel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * ViewModel for the StoreActivity.
 */
@Stable
@Suppress("LongParameterList", "TooManyFunctions")
@KoinViewModel
class StoreViewModel(
    application: Application,
    lazyStoreRepository: Lazy<StoreRepository>,
    lazySdkConfigurationListRepository: Lazy<SdkConfigurationListRepository>,
    lazyChatSettingsRepository: Lazy<ChatSettingsRepository>,
    lazyUiSettingsRepository: Lazy<UISettingsRepository>,
    /** Persistence for extra custom fields. */
    private val extraCustomFieldRepository: ExtraCustomFieldRepository,
    /** chat repository containing current chat. */
    val chatProvider: ChatInstanceProvider,
    /** environments a launch intent may name. */
    launchEnvironments: LaunchEnvironments,
    /** configuration supplied by a launch intent, if any. */
    private val pendingLaunchConfiguration: PendingLaunchConfiguration,
    /** logger for store messages. */
    logger: Logger,
) : AndroidViewModel(application), LoggerScope by LoggerScope(TAG, logger) {

    private var authService: AuthorizationService? = null
    private val uiStateStore = MutableStateFlow<UiState>(Initial)

    /**
     * Chains every writer of this field into a FIFO sequence, with init at the head: each launches
     * independently on viewModelScope, so without joining the previously-launched session job a
     * rapid second call could let its chatProvider.signOut()/configure()/prepare() start before the
     * first one's suspend call has returned. Written only from the main thread — the constructor and
     * UI actions — so plain read-then-reassign here is race-free.
     */
    private var sessionJob: Job? = null

    /** chat configuration and settings repository containing the current configuration. */
    val chatSettingsRepository: ChatSettingsRepository by lazyChatSettingsRepository

    /** UI settings repository saving and managing UI configuration. */
    val uiSettingsRepository: UISettingsRepository by lazyUiSettingsRepository

    /** store repository containing cart and product information. */
    val storeRepository: StoreRepository by lazyStoreRepository

    /** sdk configuration repository containing list of predefined SDK configurations. */
    val sdkConfigurationListRepository: SdkConfigurationListRepository by lazySdkConfigurationListRepository

    private val context
        get() = getApplication() as Context

    /** Page view handler managing page view and page view ended events. */
    val analyticsHandler = AnalyticsHandler(chatProvider, this, viewModelScope)

    /** Chat settings handler to manage settings updates into provider. */
    val chatSettingsHandler = ChatSettingsHandler(context, chatProvider, chatSettingsRepository, this)

    /** What the automated launch path is currently showing. */
    val launchPresentation = LaunchPresentation(pendingLaunchConfiguration)

    /** Handler driving a chat which was configured by a launch intent rather than through the dialogs. */
    val automatedSetupHandler = AutomatedSetupHandler(
        chatProvider,
        chatSettingsHandler,
        launchEnvironments,
        launchPresentation,
        ::setUiState,
        viewModelScope,
        this,
    )

    /** Current UI State. */
    @Stable
    val uiState = uiStateStore.asStateFlow()

    /** listener to chat instance changes. */
    private val listener = Listener().also(chatProvider::addListener)

    /**
     * In-flight token request future: set when the SDK background thread calls the delegate
     * (connect() already running), cleared when the Activity resolves it via [deliverImplicitToken].
     */
    private val pendingTokenFuture = AtomicReference<CompletableFuture<OAuthToken>?>(null)

    /**
     * True while the user has initiated implicit OAuth from the bottom sheet but the JWT has
     * not yet been delivered. Prevents onResume() from re-showing the OAuth sheet.
     */
    private val implicitAuthInProgress = AtomicBoolean(false)

    init {
        chatProvider.setTokenDelegateListener { reason ->
            scope("onNewTokenRequested") {
                debug("SDK requires a token (reason=$reason)")

                // For initial connect, return cached token immediately if available.
                val cachedToken = chatSettingsHandler.getCachedConnectionToken()
                if (cachedToken != null && reason == UNSPECIFIED) {
                    debug("Returning cached JWT for initial connect")
                    return@scope OAuthToken(cachedToken, chatSettingsHandler.getCachedConnectionTokenExpiry())
                }

                // For TOKEN_EXPIRED or TOKEN_INVALID, perform a silent refresh using the cached
                // AppAuth AuthState (which holds the refresh_token). No UI is shown to the user.
                if (reason == TOKEN_EXPIRED || reason == TOKEN_INVALID) {
                    val authStateJson = chatSettingsHandler.getAuthStateJson()
                    if (authStateJson != null) {
                        debug("Attempting silent token refresh via AuthState (reason=$reason)")
                        try {
                            return@scope performSilentTokenRefresh(authStateJson)
                        } catch (e: TokenDelegationFailedException) {
                            debug("Silent token refresh failed (${e.message}) — falling through to browser flow")
                        }
                    } else {
                        debug("No AuthState available for silent refresh — falling through to browser flow")
                    }
                }

                // No cached token and no AuthState: launch the browser sign-in flow.
                chatSettingsHandler.updateConnectionTokenOnly(null)
                debug("Requesting fresh JWT from OAuth provider via browser (reason=$reason)")
                val newFuture = CompletableFuture<OAuthToken>()
                // Only register newFuture if no request is already in-flight; reuse the existing
                // one on concurrent delegate calls so neither SDK thread is orphaned.
                pendingTokenFuture.compareAndSet(null, newFuture)
                val future = pendingTokenFuture.get() ?: newFuture
                // Browser sign-in can't be supplied on an intent, so an automated setup can't drive
                // an authorization-enabled channel any further -- hand over to the OAuth flow.
                automatedSetupHandler.abandon("the channel requires interactive authorization")
                setUiState(ImplicitOAuth)
                try {
                    val oauthToken = future.get()
                    chatSettingsHandler.updateConnectionTokenOnly(oauthToken.accessToken, oauthToken.expiryDateMillis)
                    oauthToken
                } catch (e: ExecutionException) {
                    val cause = e.cause ?: e
                    throw cause as? TokenDelegationFailedException
                        ?: TokenDelegationFailedException(
                            message = cause.message ?: "Token request cancelled",
                            cause = cause,
                        )
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw TokenDelegationFailedException("Token request interrupted", e)
                }
            }
        }
    }

    /** True if the ViewModel has completed initial loading. */
    private var isLoaded = AtomicBoolean(false)

    /** True if we have a pending listener to be called onResume(). */
    private var pendingListener = AtomicBoolean(false)

    init {
        // Assigned, so that a launch intent arriving mid-initialisation queues behind these loads
        // instead of resolving a bundled environment name against a list that has yet to load, and
        // so that two starts never apply a configuration at once.
        sessionJob = viewModelScope.launch(Dispatchers.IO) {
            // Loaded before the launch request is acted on: an intent naming an environment can only
            // be resolved once the bundled definitions are available.
            sdkConfigurationListRepository.load()
            val storedConfiguration = chatSettingsRepository.load()?.sdkConfiguration
            val launchRequest = pendingLaunchConfiguration.consume()

            // A supplied configuration replaces the stored one, so preparing that first is wasted.
            val suppliesConfiguration = launchRequest?.configuration != null
            if (!suppliesConfiguration) {
                prepareStoredConfiguration(storedConfiguration)
            }

            val drivenAutomatically = launchRequest != null &&
                    automatedSetupHandler.start(launchRequest, storedConfiguration)

            // Refused, so the startup it displaced still has to happen.
            if (suppliesConfiguration && !drivenAutomatically) {
                prepareStoredConfiguration(storedConfiguration)
            }

            uiSettingsRepository.load()
            isLoaded.set(true)

            // If onResume() was called during initialization, start chat now
            if (pendingListener.get()) {
                startChat()
                pendingListener.set(false)
            }
        }
    }

    /**
     * Bring the chat provider in line with the persisted configuration, requesting one from the user
     * if none has been stored yet.
     *
     * @param storedConfiguration Configuration loaded from storage, or null if there is none.
     */
    private suspend fun prepareStoredConfiguration(storedConfiguration: SdkConfiguration?) {
        val chatConfig = storedConfiguration?.asSocketFactoryConfiguration
        if (chatConfig == null) {
            setUiState(Configuration)
        } else if (chatProvider.chatState == ChatState.Initial) {
            verbose("Preparing chat with settings, detected state: ${chatProvider.chatState}")
            chatProvider.prepare(context, chatConfig)
        } else {
            val currentConfiguration = chatProvider.configuration
            if (currentConfiguration != chatConfig) {
                debug(
                    "Updating chat configuration," +
                            " detected state: ${chatProvider.chatState}," +
                            " configuration: $currentConfiguration," +
                            " new configuration: $chatConfig"
                )
                chatProvider.configure(context) { configuration = chatConfig }
            } else {
                verbose("Chat is configured")
            }
        }
    }

    /**
     * Apply a configuration supplied by a launch intent to an app which is already running.
     *
     * @param request What the launch intent asked for.
     */
    fun applyLaunchRequest(request: StoreLaunchRequest) {
        val previous = sessionJob
        sessionJob = viewModelScope.launch {
            previous?.join()
            val storedConfiguration = chatSettingsRepository.settings.value?.sdkConfiguration
            // A refused request changes nothing about the chat, so the app is left needing whatever
            // the stored configuration implies -- the same recovery a cold start performs.
            if (!automatedSetupHandler.start(request, storedConfiguration)) {
                prepareStoredConfiguration(storedConfiguration)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        deliverImplicitTokenFailure(TokenDelegationFailedException("ViewModel cleared"))
        chatProvider.removeListener(listener)
        chatProvider.setTokenDelegateListener(null)
        authService?.dispose()
        authService = null
    }

    /**
     * Update UI State.
     *
     * @param state new UI State.
     */
    fun setUiState(state: UiState) = scope("setUiState") {
        debug("uiState: ${uiState.value} -> $state")
        if (state == ImplicitOAuth) {
            implicitAuthInProgress.set(true)
        }
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
     * Set the sdk configuration to use for future attempts.
     *
     * @param sdkConfiguration New configuration to use.
     */
    fun setConfiguration(sdkConfiguration: SdkConfiguration) {
        val previous = sessionJob
        sessionJob = viewModelScope.launch {
            previous?.join()
            chatSettingsHandler.setConfiguration(sdkConfiguration)
        }
    }

    /**
     * Set the authorization to use for future connections.
     *
     * @param authorization New authorization to use.
     */
    fun setAuthorization(authorization: Authorization) {
        val previous = sessionJob
        sessionJob = viewModelScope.launch {
            previous?.join()
            chatSettingsHandler.setAuthorization(authorization)
        }
    }

    /**
     * Set the login data for future connections.
     *
     * @param loginData New login data to use.
     */
    fun setLoginData(loginData: LoginData) {
        val previous = sessionJob
        sessionJob = viewModelScope.launch {
            previous?.join()
            chatSettingsHandler.setLoginData(loginData)

            if (chatProvider.chatState == ChatState.Initial) {
                chatProvider.prepare(context)
            } else {
                listener.onChatStateChanged(chatProvider.chatState)
            }
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

        true -> if (settings?.authorization != null || settings?.connectionToken != null) {
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
     * Performs a silent token refresh using AppAuth's [AuthState.performActionWithFreshTokens].
     * Blocks the calling thread until the refresh completes. Updates the persisted [AuthState]
     * and token cache on success.
     */
    private fun performSilentTokenRefresh(authStateJson: String): OAuthToken {
        val authState = AuthState.jsonDeserialize(authStateJson)
        val future = CompletableFuture<OAuthToken>()
        val service = authService ?: AuthorizationService(getApplication<Application>()).also { authService = it }
        authState.performActionWithFreshTokens(service) { accessToken, _, ex ->
            when {
                ex != null -> future.completeExceptionally(
                    TokenDelegationFailedException("Silent token refresh failed: ${ex.message}", ex)
                )

                accessToken != null -> {
                    val expiryMillis = authState.accessTokenExpirationTime
                    chatSettingsHandler.saveAuthStateJson(authState.jsonSerializeString())
                    chatSettingsHandler.updateConnectionTokenOnly(accessToken, expiryMillis)
                    future.complete(OAuthToken(accessToken, expiryMillis))
                }

                else -> future.completeExceptionally(
                    TokenDelegationFailedException("Silent token refresh returned no access token")
                )
            }
        }
        return try {
            future.get()
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            throw cause as? TokenDelegationFailedException
                ?: TokenDelegationFailedException(cause.message ?: "Silent refresh failed", cause)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TokenDelegationFailedException("Silent token refresh interrupted", e)
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
        val previous = sessionJob
        sessionJob = viewModelScope.launch {
            previous?.join()
            chatSettingsHandler.clearAuthentication()
            extraCustomFieldRepository.clear(context)
            // This needs to be *after* all the settings are cleared out or we
            // immediately reconnect using the same information.
            chatProvider.signOut()
        }
    }

    /**
     * Called by hosting [android.app.Activity.onResume] to generate appropriate page view events when
     * the application is resumed and to restart the chat sdk and enable analytics.
     */
    fun onResume() {
        // Being resumed while the chat was being opened means the chat has been left again, which is
        // the point at which the store becomes worth building. Skipped while the request is still
        // pending: the state was published while the activity was stopped, so the effect which opens
        // the chat has not run yet and clearing it here would drop the launch on the floor.
        if (uiState.value === OpeningChat && !launchPresentation.isOpenChatPending) {
            launchPresentation.showStorefront()
            setUiState(Prepared)
        }
        if (isLoaded.get()) {
            // Skip reconnect while a token request is in-flight (SDK background thread blocked)
            // or while the user is in the middle of the initial implicit OAuth sign-in flow.
            // In both cases onResume fires before the JWT is delivered; calling startChat() here
            // would trigger a second connect() and cause an infinite loop.
            if (pendingTokenFuture.get() != null || implicitAuthInProgress.get()) return
            startChat()
            return
        }
        pendingListener.set(true)
    }

    private inner class Listener : ChatInstanceProvider.Listener {
        /**
         * Send a pending page view if chat is just now established.
         */
        override fun onChatStateChanged(chatState: ChatState) {
            // An automated setup owns the states it can drive, so the configuration and login dialogs
            // don't interrupt an unattended run. States it can't -- a failed prepare, an unsupported
            // SDK -- are handed back here and answered exactly as they always were.
            if (automatedSetupHandler.onChatStateChanged(chatState)) return
            // If the chat has now connected, see if we need to send authorization
            when (chatState) {
                ChatState.Initial -> setUiState(Configuration)
                ChatState.Preparing -> setUiState(Preparing)
                ChatState.Prepared -> onConnected()
                ChatState.SdkNotSupported -> setUiState(SdkNotSupported)
                else -> Ignored
            }
        }

        override fun onChatRuntimeException(exception: RuntimeChatException) = scope("onChatRuntimeException") {
            when (exception) {
                is RuntimeChatException.ConnectionTokenFailed -> {
                    val isAuthorizationEnabled = chatProvider.chat?.configuration?.isAuthorizationEnabled
                    val state = currentUiState(this, null, isAuthorizationEnabled) ?: return@scope
                    val currentState = uiState.value
                    if (!(state == Prepared && (currentState is UiSettings || currentState is Configuration))) {
                        setUiState(state)
                    }
                }
                else -> error("Chat SDK reported exception.", exception)
            }
        }
    }

    /**
     * Called by [StoreActivity] once it has obtained a JWT from the OAuth provider.
     *
     * Two cases:
     * 1. A connect() is already in-flight (pendingTokenFuture is set) — the SDK background thread
     *    is blocked waiting for the JWT. Complete the future to unblock it; the delegate persists
     *    the token after future.get() returns.
     * 2. No connect() in flight (initial OAuth flow: user just completed sign-in from the OAuth
     *    sheet before connect() was called) — persist the token now and call connect() so the SDK
     *    background thread starts and the delegate immediately returns the cached token.
     *
     * @param token The JWT access token.
     * @param expiryMillis Unix epoch millis at which [token] expires, or `null` if unknown.
     */
    fun deliverImplicitToken(token: String, expiryMillis: Long?) {
        implicitAuthInProgress.set(false)
        val oauthToken = OAuthToken(token, expiryMillis)
        val future = pendingTokenFuture.getAndSet(null)
        if (future != null) {
            // connect() already in-flight (mid-session refresh) — unblock the SDK background thread
            future.complete(oauthToken)
        } else {
            // Initial OAuth — persist the token so the delegate can return it immediately when
            // connect() is called (by ChatViewModel when the user opens ChatActivity via the FAB).
            chatSettingsHandler.updateConnectionTokenOnly(token, expiryMillis)
        }
    }

    /**
     * Called by [StoreActivity] when the OAuth provider fails to supply a JWT.
     * Wraps [cause] in [TokenDelegationFailedException] if needed and
     * completes the pending future exceptionally so the SDK background thread unblocks.
     */
    fun deliverImplicitTokenFailure(cause: Throwable) {
        implicitAuthInProgress.set(false)
        val exception = cause as? TokenDelegationFailedException
            ?: TokenDelegationFailedException(cause.message ?: "Token request failed", cause)
        pendingTokenFuture.getAndSet(null)?.completeExceptionally(exception)
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private const val TAG = "StoreViewModel"
    }
}
