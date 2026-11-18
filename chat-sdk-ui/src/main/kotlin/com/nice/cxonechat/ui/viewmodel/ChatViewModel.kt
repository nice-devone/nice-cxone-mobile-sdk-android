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

package com.nice.cxonechat.ui.viewmodel

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.domain.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.domain.model.CreateThreadResult.Success
import com.nice.cxonechat.ui.domain.model.NoThread
import com.nice.cxonechat.ui.domain.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.util.isAtLeastPrepared
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.None
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Preparing
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Survey
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.ThreadCreationFailed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooManyFunctions")
@KoinViewModel
internal class ChatViewModel(
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chatProvider: ChatInstanceProvider,
    @Provided
    @Named(UiModule.LOGGER_NAME)
    private val logger: Logger,
    private val chatStateViewModel: ChatStateViewModel,
) : ViewModel(), LoggerScope by LoggerScope("ChatViewModel", logger) {
    private val chat: Chat
        get() = requireNotNull(chatProvider.chat)

    private val uiCustomerFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Customer),
    )
    private val uiContactFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Contact),
    )

    private val threads: ChatThreadsHandler
        get() = chat.threads()

    /** Possible dialog states. */
    sealed interface DialogState {
        /** No dialog should be shown. */
        data object None : DialogState

        /** Progress dialog should be shown until loading is finished. */
        data object Preparing : DialogState

        /** Show pre-chat survey dialog. */
        data class Survey(val survey: PreChatSurvey) : DialogState

        /** Show dialog with error message. */
        data class ThreadCreationFailed(val failure: Failure) : DialogState
    }

    @VisibleForTesting
    internal var resolveStateJob: Job? = null

    private var reportOnResumeJob: Job? = null

    private val showDialog by lazy { MutableStateFlow<DialogState>(refreshDialogState()) }
    val dialogShown by lazy {
        viewModelScope.launch {
            chatStateViewModel.state.collect { state ->
                if (state === ChatState.Offline && showDialog.value === Preparing) {
                    verbose("Chat is offline, dismissing preparing dialog")
                    dismissDialog()
                }
            }
        }
        showDialog.asStateFlow()
    }

    val chatMode: ChatMode
        get() = chat.chatMode

    val preChatSurvey
        get() = threads.preChatSurvey

    /** Prevent interaction with possibly uninitialized conversation in single/livechat mode or non-ready chat. */
    private fun refreshDialogState(): DialogState =
        if (
            chatStateViewModel.state.value == ChatState.Ready &&
            (chatMode === ChatMode.MultiThread || preChatSurvey == null)
        ) {
            None
        } else {
            Preparing
        }

    internal fun refreshThreadState() = scope("refreshThreadState") {
        showDialog.value = refreshDialogState().also {
            verbose("Refreshed dialog state: $it")
        }
        val previousJob = resolveStateJob?.apply { cancel() }
        resolveStateJob = viewModelScope.launch {
            previousJob?.join()
            try {
                resolveCurrentState()
            } catch (e: CancellationException) {
                throw e
            } catch (logged: Exception) {
                error("Failed to resolve thread state", logged)
                dismissDialog()
            }
        }
    }

    private fun createThread(): Unit = scope("createThread") {
        createThread(emptySequence())
    }

    internal fun respondToSurvey(response: Sequence<PreChatResponse>): Unit = scope("respondToSurvey") {
        createThread(response)
    }

    private fun createThread(response: Sequence<PreChatResponse>) = scope("createThread") {
        viewModelScope.launch {
            val result = runCatching {
                setCustomFields()
                val handler = threads.create(
                    customFields = withContext(Dispatchers.IO) {
                        uiContactFieldsProvider.customFields()
                    },
                    preChatSurveyResponse = response,
                )
                selectedThreadRepository.chatThreadHandler = handler
            }.foldToCreateThreadResult()

            when (result) {
                is Failure -> showDialog(ThreadCreationFailed(result))
                Success -> {
                    verbose("Thread created successfully, dismissing dialog")
                    dismissDialog()
                }
            }
        }
    }

    private suspend fun resolveCurrentState() = timedScope("resolveCurrentState") {
        verbose(
            "Resolving current state, chatMode: $chatMode, chatState: ${chatProvider.chatState}," +
                " preChatSurveyPresent: ${preChatSurvey != null}"
        )
        withContext(Dispatchers.Default) {
            if (listOf(SingleThread, LiveChat).contains(chat.chatMode)) {
                if (chatProvider.chatState === ChatState.Offline) {
                    verbose("Chat is offline, dismissing dialog")
                    dismissDialog()
                    return@withContext
                }
                resolveThreadOrSurvey()
            }
        }
    }

    private suspend fun resolveThreadOrSurvey() = timedScope("resolveThreadOrSurvey") {
        when {
            selectFirstThread() -> {
                verbose("Selected existing thread, dismissing dialog")
                dismissDialog()
            }

            else -> {
                verbose("No existing thread selected, checking pre-chat survey")
                when (val chatSurvey = preChatSurvey) {
                    null -> {
                        verbose("No pre-chat survey configured, creating thread")
                        createThread()
                    }

                    else -> {
                        verbose("Pre-chat survey configured, waiting for any pending satisfaction survey first")
                        // Preparing renders in its own modal window below API 30, which could cover
                        // the already-showing survey's Cancel control for as long as this waits.
                        if (selectedThreadRepository.isSatisfactionSurveyShowing) dismissDialog()
                        selectedThreadRepository.satisfactionSurveyShowingFlow.first { !it }
                        verbose("Showing pre-chat survey dialog")
                        showDialog(Survey(chatSurvey))
                    }
                }
            }
        }
    }

    /**
     * We're in single thread mode.  Select the first thread if it exists.
     *
     * Returns true iff the initial thread exists and was selected.
     */
    private suspend fun selectFirstThread(): Boolean = timedScope("selectFirstThread") {
        val currentThread = selectedThreadRepository.chatThreadHandler.get()

        if (currentThread !== NoThread) {
            if (currentThread.threadState === ChatThreadState.Pending) {
                verbose("Currently selected thread is pending, recreating handler")
                selectedThreadRepository.chatThreadHandler = threads.thread(currentThread)
                return true
            }
            // This also runs on every resume, not just "New Conversation" - a thread whose
            // EndContact was not shown yet (survey still pending/showing) must be left as is.
            // Once EndContact was shown, its lifecycle is done - resolve a fresh one instead.
            val endContactAlreadyShown = selectedThreadRepository.withEndContactLock {
                selectedThreadRepository.getEndContactShownForThread()
            } == currentThread.id.toString()
            if (!endContactAlreadyShown) {
                verbose("Thread is already selected and not fully resolved yet, leaving it as is")
                return true
            }
            verbose("EndContact was already shown for the selected thread, resolving a fresh one")
        }

        // The thread we are trying to replace (null on a genuine cold start, where there is
        // nothing to be stale yet).
        val staleThreadId = currentThread.takeIf { it !== NoThread }?.id

        verbose("Starting thread list refresh")
        val flow = threads.threadsFlow
        verbose("Triggering threads refresh")
        threads.refresh()

        // threadsFlow replays its last (stale) value to a new collector, so without a stale
        // thread to replace (cold start) take the first emission as-is; otherwise wait for one
        // that actually differs, instead of racing refresh() with what was already cached.
        // Bounded: some recovery outcomes (e.g. a suppressed failure, or handleLiveChatRecovered
        // throwing) never emit at all, which would otherwise wait forever with no way out.
        return withTimeoutOrNull(THREAD_REFRESH_TIMEOUT) {
            flow.first { staleThreadId == null || it.firstOrNull()?.id != staleThreadId }
        }
            ?.firstOrNull()
            ?.let {
                verbose("Selecting existing thread with id ${it.id}")
                selectedThreadRepository.chatThreadHandler = threads.thread(it)
                true
            } == true
    }

    private suspend fun setCustomFields() = scope("setCustomFields") {
        withContext(Dispatchers.IO) {
            chatProvider.setCustomerValues(uiCustomerFieldsProvider.customFields())
        }
    }

    /**
     * Once the chat is Ready, adds the customer custom fields and reports `chatWindowOpen()`, which can trigger the
     * welcome message; a chat replaced meanwhile (e.g. by `signOut()`) is skipped, best effort.
     *
     * It waits for Ready, not just Prepared, or the backend drops the welcome message trigger. The fields go first, so a
     * message sent during the report carries them. A repeated call replaces the pending one, so a resume is reported
     * once, and [cancelReportOnResume] stops it, including a request in flight.
     */
    internal fun reportOnResume() = scope("reportOnResume") {
        reportOnResumeJob?.cancel()
        reportOnResumeJob = viewModelScope.launch(Dispatchers.IO) {
            duration {
                chatStateViewModel.state.first { it === ChatState.Ready }
                val reportingChat = chatProvider.chat ?: return@duration
                addCustomerFields(reportingChat)
                // A replaced chat is closed: reporting to it could refresh the token of the cleared session.
                if (chatProvider.chat !== reportingChat) return@duration
                reportChatWindowOpen(reportingChat)
            }
        }
    }

    /**
     * Skips a chat replaced meanwhile (signOut()): the fields belong to the session they were read for.
     * Best effort — the chat can still be replaced between the check and add().
     */
    private fun addCustomerFields(reportingChat: Chat) {
        try {
            val extraCustomValues = uiCustomerFieldsProvider.customFields()
            if (extraCustomValues.isNotEmpty() && chatProvider.chat === reportingChat) {
                reportingChat.customFields().add(extraCustomValues)
            }
        } catch (expected: CancellationException) {
            throw expected // a provider signalling cancellation ends the report, as a cancelled request does
        } catch (logged: Exception) {
            warning("Failed to add the customer custom fields", logged)
        }
    }

    /** A failed report is logged rather than left to crash the app; a cancelled one ends quietly. */
    private suspend fun reportChatWindowOpen(reportingChat: Chat) {
        try {
            reportingChat.events().chatWindowOpen()
        } catch (expected: CancellationException) {
            throw expected
        } catch (logged: Exception) {
            warning("Failed to report the chat window open", logged)
        }
    }

    /** Stops waiting to report a chat-window open — call when the screen is no longer resumed. */
    internal fun cancelReportOnResume() = scope("cancelReportOnResume") {
        reportOnResumeJob?.cancel()
        reportOnResumeJob = null
    }

    private fun showDialog(dialog: DialogState) = scope("showDialog") {
        verbose("Showing dialog: $dialog")
        showDialog.value = dialog
    }

    private fun dismissDialog() = scope("dismissDialog") {
        verbose("Dismissing dialog ${showDialog.value}")
        showDialog.value = None
    }

    internal fun prepare(context: Context) = scope("prepare") {
        val chatState = chatProvider.chatState
        if (!chatState.isAtLeastPrepared()) {
            showDialog(Preparing)
        }
        if (chatState === ChatState.Initial) {
            // This can happen on low powered devices when user has to grant permissions or interact with system dialogs
            viewModelScope.launch(Dispatchers.Default) {
                while (chatProvider.configuration == null) {
                    // Wait for application to set the configuration
                    // If it never happens user can cancel preparing dialog and viewModelScope will be terminated
                    delay(150L)
                }
                // Check if state is still Initial and start it up
                if (chatProvider.chatState === ChatState.Initial) chatProvider.prepare(context)
            }
        } else {
            warning("Chat already prepared, currentState: $chatState")
        }
    }

    internal fun connect(): Unit = timedScope("connect") {
        val chatState = chatProvider.chatState
        if (chatState === ChatState.Connecting) {
            verbose("SDK is already reconnecting autonomously, showing loading overlay")
            showDialog(Preparing)
            return
        }
        if (chatState !== ChatState.Prepared && chatState !== ChatState.ConnectionLost && chatState !== ChatState.Offline) {
            error("Unable to connect chat in state: $chatState")
            return
        }
        showDialog(Preparing)
        chatProvider.connect()
    }

    internal fun onReady() = scope("onReady") {
        if (showDialog.value === Preparing) {
            verbose("Chat is ready, dismissing preparing dialog")
            dismissDialog()
        }
    }

    internal suspend fun close() = scope("close") {
        verbose("Closing chat, current state: ${chatProvider.chatState}")
        dismissDialog() // dismiss dialog if present when closing chat -- unconditional, before the suspend call below
        // Runs to completion even if the caller's scope (e.g. Activity.lifecycleScope) is cancelled
        // mid-drain -- storage/cookie cleanup must finish once started, not be cut short by ON_DESTROY.
        withContext(NonCancellable) {
            chatProvider.closeSuspending()
        }
    }

    internal companion object {
        /** Mirrors ChatThreadsHandlerMulti.METADATA_LOAD_TIMEOUT for a similar recovery wait. */
        val THREAD_REFRESH_TIMEOUT = 30_000L.milliseconds
    }
}
