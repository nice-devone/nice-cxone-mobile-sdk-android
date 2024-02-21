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

package com.nice.cxonechat.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode.MULTI_THREAD
import com.nice.cxonechat.ChatMode.SINGLE_THREAD
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.MultiThreadEnabled
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.NavigationFinished
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.SingleThreadCreated
import com.nice.cxonechat.ui.main.ChatViewModel.State.CreateSingleThread
import com.nice.cxonechat.ui.main.ChatViewModel.State.Initial
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadCreationFailed
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.model.CreateThreadResult.Success
import com.nice.cxonechat.ui.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.getCustomerCustomValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@Suppress("TooManyFunctions")
@KoinViewModel
internal class ChatViewModel(
    private val valueStorage: ValueStorage,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chatProvider: ChatInstanceProvider,
) : ViewModel() {
    private val chat: Chat
        get() = requireNotNull(chatProvider.chat)

    private val threads by lazy { chat.threads() }

    private val events by lazy { chat.events() }

    private val internalState: MutableStateFlow<State> = MutableStateFlow(Initial)

    sealed interface Dialogs {
        data object None : Dialogs
        class Survey(val survey: PreChatSurvey) : Dialogs
    }

    private val showDialog = MutableStateFlow<Dialogs>(None)
    val dialogShown = showDialog.asStateFlow()

    val isMultiThreadEnabled: Boolean
        get() = chat.chatMode == MULTI_THREAD

    val state
        get() = internalState
            .asStateFlow()
            .onSubscription {
                internalState.value = resolveCurrentState()
            }

    val preChatSurvey
        get() = threads.preChatSurvey

    internal fun setNavigationFinishedState() {
        internalState.value = NavigationFinished
    }

    internal fun dismissThreadCreationFailure() {
        viewModelScope.launch {
            internalState.value = resolveCurrentState()
        }
    }

    internal fun createThread() {
        createThread(emptySequence())
    }

    internal fun respondToSurvey(response: Sequence<PreChatResponse>) {
        createThread(response)
    }

    private fun createThread(response: Sequence<PreChatResponse>) {
        viewModelScope.launch {
            val result = runCatching {
                setCustomFields()
                val handler = threads.create(preChatSurveyResponse = response)
                selectedThreadRepository.chatThreadHandler = handler
            }.foldToCreateThreadResult()

            when (result) {
                is Failure -> internalState.value = SingleThreadCreationFailed(result)
                Success -> {
                    internalState.value = SingleThreadCreated
                    dismissDialog()
                }
            }
        }
    }

    private suspend fun resolveCurrentState(): State {
        if (internalState.value == NavigationFinished) return NavigationFinished

        return when (chat.chatMode) {
            MULTI_THREAD -> MultiThreadEnabled
            SINGLE_THREAD -> singleThreadChatState()
        }
    }

    /**
     * Determine the correct state for a single thread chat.
     *
     * when:
     *
     * - there's already a thread, use [SingleThreadCreated]
     * - there's a survey, use [SingleThreadPreChatSurveyRequired]
     * - otherwise use [CreateSingleThread]
     */
    private suspend fun singleThreadChatState() = if (selectFirstThread()) {
        SingleThreadCreated
    } else {
        preChatSurvey?.let(::SingleThreadPreChatSurveyRequired) ?: CreateSingleThread
    }

    /**
     * We're in single thread mode.  Select the first thread if it exists.
     *
     * Returns true iff the initial thread exists and was selected.
     */
    private suspend fun selectFirstThread(): Boolean {
        val flow = threads.flow
        threads.refresh()

        return flow.first().firstOrNull()?.let {
            selectedThreadRepository.chatThreadHandler = threads.thread(it)
            true
        } ?: false
    }

    private suspend fun setCustomFields() {
        chatProvider.setCustomerValues(valueStorage.getCustomerCustomValues())
    }

    internal fun reportOnResume() {
        events.chatWindowOpen()
    }

    private fun showDialog(dialog: Dialogs) {
        showDialog.value = dialog
    }

    private fun dismissDialog() {
        showDialog.value = None
    }

    internal fun showPreChatSurvey(survey: PreChatSurvey) {
        showDialog(Survey(survey))
    }

    internal fun prepare(context: Context) {
        chatProvider.prepare(context)
    }

    internal fun connect() {
        chatProvider.connect()
    }

    internal fun close() {
        chatProvider.close()
    }

    /**
     * Definition of navigation states for the view model.
     */
    sealed interface NavigationState : State {
        /**
         * Navigation should be directed to multi-thread flow.
         */
        data object MultiThreadEnabled : NavigationState

        /**
         * Navigation should be directed to single thread chat.
         */
        data object SingleThreadCreated : NavigationState

        /**
         * Final state of navigation, either the activity has successfully navigated to [MultiThreadEnabled]
         * state or [SingleThreadCreated] state.
         */
        data object NavigationFinished : NavigationState
    }

    /**
     * Definition of states for the view model.
     */
    sealed interface State {
        /**
         * Default state.
         */
        data object Initial : State

        /**
         * Single thread is ready to be created.
         */
        data object CreateSingleThread : State

        /**
         * Single thread creation requires prechat survey to be finished first, before thread can be created.
         * @property survey Survey which should be presented to the user.
         */
        data class SingleThreadPreChatSurveyRequired(val survey: PreChatSurvey) : State

        /**
         * Single thread creation has resulted in an error.
         *
         * @property failure What type of failure has happened.
         */
        data class SingleThreadCreationFailed(val failure: Failure) : State
    }
}
