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

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.sample.data.models.ChatUserName
import com.nice.cxonechat.sample.data.models.DefaultChatUser
import com.nice.cxonechat.sample.data.models.LaunchExtras
import com.nice.cxonechat.sample.data.models.LoginData
import com.nice.cxonechat.sample.data.models.RequestedEnvironment
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.StoreEnvironment
import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import com.nice.cxonechat.sample.data.repository.LaunchEnvironments
import com.nice.cxonechat.sample.extensions.Ignored
import com.nice.cxonechat.sample.viewModel.UiState.AutomatedSetup
import com.nice.cxonechat.sample.viewModel.UiState.AutomatedSetup.Step
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Drives the chat from an externally supplied launch request all the way to a ready connection,
 * without any of the dialogs a person would otherwise have to work through.
 *
 * While a setup is running this owns [UiState], showing progress with a cancel button so that a
 * person using the same entry point by hand isn't trapped behind a spinner. It only claims the chat
 * states it can actually drive: anything it can't -- a failed prepare, an unsupported SDK -- is
 * handed straight back, so the dialogs the app has always shown for those still appear.
 *
 * @param chatProvider Provider to configure, connect and report progress for.
 * @param chatSettingsHandler Handler used to apply the requested configuration and login.
 * @param launchEnvironments Environments a named request can be resolved against.
 * @param presentation What this path shows while it drives.
 * @param setUiState Invoked to publish the current progress.
 * @param coroutineScope [CoroutineScope] used to start the connection off the provider's listener callback.
 * @param logger [Logger] used as base for [LoggerScope].
 */
@Suppress("LongParameterList")
class AutomatedSetupHandler(
    private val chatProvider: ChatInstanceProvider,
    private val chatSettingsHandler: ChatSettingsHandler,
    private val launchEnvironments: LaunchEnvironments,
    private val presentation: LaunchPresentation,
    private val setUiState: (UiState) -> Unit,
    private val coroutineScope: CoroutineScope,
    logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {

    private val active = AtomicBoolean(false)
    private val openChatWhenReady = AtomicBoolean(false)
    private val connectRequested = AtomicBoolean(false)
    private val reconnects = AtomicInteger(0)

    /**
     * True while the provider is being reconfigured or cancelled, which briefly reports [ChatState.Initial]
     * on the way. Outside that window Initial means the chat failed to prepare, which is not ours to drive.
     */
    private val expectingTransientInitial = AtomicBoolean(false)

    /** In-flight connection attempt, cancelled when this handler stops driving. */
    private val connectJob = AtomicReference<Job?>(null)

    /** True while this handler owns [UiState] and is driving the chat towards a ready connection. */
    val isActive: Boolean
        get() = active.get()

    /**
     * Apply [request] and start driving the chat towards a ready connection.
     *
     * @param request What the launch intent asked for.
     * @param storedConfiguration Configuration already persisted, if any. A request which carries no
     * configuration of its own can only be honoured when there is one of these to open.
     * @return True if this handler took the app over, false if the request could not be used and the
     * caller should carry on with the startup it would otherwise have done.
     */
    suspend fun start(request: StoreLaunchRequest, storedConfiguration: SdkConfiguration?): Boolean = scope("start") {
        val configuration = request.configuration?.let { requested ->
            // Resolved here rather than while the intent was read: the bundled environment
            // definitions had not been loaded yet at that point.
            val environment = when (val wanted = requested.environment) {
                is RequestedEnvironment.Custom -> wanted.environment
                is RequestedEnvironment.Named -> launchEnvironments.byName(wanted.name) ?: run {
                    warning(
                        "Ignoring launch intent: unknown '${LaunchExtras.ENVIRONMENT}' value '${wanted.name}', " +
                                "expected one of ${launchEnvironments.availableNames.joinToString()} " +
                                "or ${StoreEnvironment.CUSTOM}"
                    )
                    refuse()
                    return@scope false
                }
            }
            requested.toSdkConfiguration(environment)
        }
        if (configuration == null && storedConfiguration == null) {
            warning("Ignoring launch intent: it supplied no configuration and none is stored, so there is nothing to prepare")
            refuse()
            return@scope false
        }
        // The parser already stands a user in whenever a channel was supplied. A request without one
        // still needs it if nobody is signed in -- after a logout the chat would connect unnamed and
        // the login dialog this handler suppresses would never get the chance to ask.
        val loginData = request.loginData ?: STAND_IN_USER.takeIf { chatSettingsHandler.userName == null }

        debug("Starting automated setup, configuration=$configuration, openChat=${request.openChat}")
        active.set(true)
        connectRequested.set(false)
        reconnects.set(0)
        openChatWhenReady.set(request.openChat)
        presentation.hideStorefront()
        setUiState(AutomatedSetup(Step.SettingUp))

        expectingTransientInitial.set(true)
        try {
            when {
                configuration != null -> chatSettingsHandler.setConfiguration(configuration, loginData)
                loginData != null -> chatSettingsHandler.setLoginData(loginData)
                else -> Ignored
            }
        } finally {
            expectingTransientInitial.set(false)
        }

        // Applying a configuration restarts the chat, which reports its progress through
        // onChatStateChanged. A request which only asked to open the chat changes no state at all,
        // so pick up from wherever the chat already is.
        onChatStateChanged(chatProvider.chatState)
        true
    }

    /**
     * React to a chat state change.
     *
     * @param chatState Newly reported chat state.
     * @return True if this handler dealt with the state, false if the caller should handle it as it
     * normally would -- either because no setup is running, or because the state is one this handler
     * deliberately doesn't drive.
     */
    fun onChatStateChanged(chatState: ChatState): Boolean = scope("onChatStateChanged") {
        if (!active.get()) return@scope false
        if (handsBack(chatState)) return@scope false

        when (chatState) {
            ChatState.Preparing -> setUiState(AutomatedSetup(Step.LoadingConfiguration))

            // Nothing else in the sample app connects the chat -- ChatActivity does it, and we need
            // the chat ready before we open it -- so the connection has to be started here.
            ChatState.Prepared -> {
                setUiState(AutomatedSetup(Step.PreparingChat))
                if (connectRequested.compareAndSet(false, true)) connect()
            }

            ChatState.Connecting, ChatState.Connected -> setUiState(AutomatedSetup(Step.PreparingChat))

            // The provider does not reconnect itself from here; ChatActivity offers an explicit action
            // for it, and an unattended run has nobody to press it.
            ChatState.ConnectionLost -> reconnect()

            ChatState.Ready -> finish()

            ChatState.Offline -> abandon("the chat is offline", UiState.Prepared)

            // Initial while a configuration is being applied is the transient teardown; anything else
            // was dealt with by handsBack above.
            else -> Ignored
        }
        true
    }

    /**
     * Give up on the automated setup at the request of the person watching it.
     *
     * [UiState.Prepared] is published before the chat is cancelled so that the resulting state change
     * can still override it -- a cancel with nothing configured ends up at the configuration dialog
     * rather than on an unusable storefront.
     */
    fun cancel() = scope("cancel") {
        if (!active.get()) return@scope
        debug("Automated setup cancelled")
        stop()
        setUiState(UiState.Prepared)
        chatProvider.cancel()
    }

    /**
     * Back out of a launch request refused before it could start.
     *
     * [abandon] clears the spinner of a setup this refusal has just torn down, which nothing else
     * would: the caller recovers by preparing the stored configuration, and that publishes no state
     * when the chat is already configured with it. On a cold start no setup is running, so [abandon]
     * does nothing and [stop] is still needed to put back the storefront hidden when the request was
     * deposited.
     */
    private fun refuse() {
        abandon("a launch intent was refused", UiState.Prepared)
        stop()
    }

    /**
     * Give up on the automated setup because something outside it needs to take over.
     *
     * @param reason Why the setup was given up on, for the log.
     * @param state State to publish, or null to leave that to whatever is taking over.
     */
    fun abandon(reason: String, state: UiState? = null) = scope("abandon") {
        if (!active.get()) return@scope
        warning("Automated setup abandoned because $reason")
        stop()
        state?.let(setUiState)
    }

    /**
     * Deal with the states this handler deliberately leaves to the rest of the app: a chat which
     * failed to prepare, and an SDK the backend refuses.
     */
    private fun handsBack(chatState: ChatState): Boolean {
        val reason = when {
            chatState === ChatState.SdkNotSupported -> "the SDK version is not supported"
            chatState === ChatState.Initial && !expectingTransientInitial.get() -> "the chat could not be prepared"
            else -> return false
        }
        warning("Automated setup handed back because $reason")
        stop()
        return true
    }

    private fun reconnect() {
        if (reconnects.incrementAndGet() > MAX_RECONNECTS) {
            abandon("the connection was lost more than $MAX_RECONNECTS times", UiState.Prepared)
            return
        }
        setUiState(AutomatedSetup(Step.PreparingChat))
        connect()
    }

    /**
     * Open the chat connection.
     *
     * Launched because the provider notifies listeners while holding the lock `connect()` advances.
     * That defers the state check to this thread, where the provider's own retry scheduler may have
     * won the race and `connect()` throws; uncaught, that would kill the process.
     */
    private fun connect() {
        // Published before the previous attempt is cancelled, so the field never drops an attempt a
        // concurrent stop() would otherwise have to cancel.
        connectJob.getAndSet(
            coroutineScope.launch {
                // A stop() landing between this launch and the getAndSet below has nothing to cancel
                // yet, so the job would outlive it. Reached from the listener thread the body is
                // queued and observes that stop(); reached from Main it inlines, and there is no
                // concurrent stop() on a single thread for it to miss.
                if (!active.get()) return@launch
                try {
                    chatProvider.connect()
                } catch (logged: InvalidStateException) {
                    debug("Connection already under way (${logged.message})")
                }
            }
        )?.cancel()
    }

    private fun finish() {
        debug("Chat is ready, automated setup complete")
        val openChat = openChatWhenReady.get()
        // Decided before stop(), not corrected after: this runs off the main thread, so a
        // true-then-false pair would let a collector observe the intermediate value.
        stop(showStorefront = !openChat)
        if (openChat) presentation.requestOpenChat()
        setUiState(if (openChat) UiState.OpeningChat else UiState.Prepared)
    }

    private fun stop(showStorefront: Boolean = true) {
        // Otherwise a connection requested a moment ago still lands after Cancel.
        connectJob.getAndSet(null)?.cancel()
        active.set(false)
        openChatWhenReady.set(false)
        connectRequested.set(false)
        reconnects.set(0)
        if (showStorefront) presentation.showStorefront()
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private const val TAG = "AutomatedSetupHandler"

        /** How many times a lost connection is retried before the setup gives up. */
        private const val MAX_RECONNECTS = 2

        /** Signed in when a setup would otherwise leave the chat without a user. */
        private val STAND_IN_USER = LoginData(
            userName = ChatUserName(lastName = DefaultChatUser.LAST_NAME, firstName = DefaultChatUser.FIRST_NAME),
            customerId = null,
        )
    }
}
