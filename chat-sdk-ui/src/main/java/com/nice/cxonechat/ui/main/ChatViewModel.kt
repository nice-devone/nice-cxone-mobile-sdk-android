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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.containsField
import com.nice.cxonechat.state.validate
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.customvalues.CustomValueItemList
import com.nice.cxonechat.ui.customvalues.extractStringValues
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.MultiThreadEnabled
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.NavigationFinished
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.SingleThreadCreated
import com.nice.cxonechat.ui.main.ChatViewModel.State.Initial
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadCreationFailed
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadCreationReady
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.model.CreateThreadResult.Success
import com.nice.cxonechat.ui.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.getCustomerCustomValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val valueStorage: ValueStorage,
    private val selectedThreadRepository: SelectedThreadRepository,
) : ViewModel() {
    private val chatProvider = ChatInstanceProvider.get()

    val chat: Chat
        get() = requireNotNull(chatProvider.chat)

    private val threads by lazy { chat.threads() }

    private val events by lazy { chat.events() }

    private val internalState: MutableStateFlow<State> = MutableStateFlow(Initial)

    val customValues: List<CustomField>
        get() = selectedThreadRepository.chatThreadHandler?.get()?.fields ?: listOf()

    val selectedThreadName: String?
        get() = selectedThreadRepository.chatThreadHandler?.get()?.threadName

    sealed interface Dialogs {
        object None : Dialogs
        object CustomValues : Dialogs
        object EditThreadName : Dialogs
        class Survey(val survey: PreChatSurvey) : Dialogs
    }

    private val showDialog = MutableStateFlow<Dialogs>(None)
    val dialogShown = showDialog.asStateFlow()

    val isMultiThreadEnabled: Boolean
        get() = chat.configuration.hasMultipleThreadsPerEndUser

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
        val preChatSurvey = threads.preChatSurvey
        if (preChatSurvey != null) {
            internalState.value = SingleThreadPreChatSurveyRequired(preChatSurvey)
            return
        }

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
        return when {
            isMultiThreadEnabled -> MultiThreadEnabled
            isFirstThread() -> SingleThreadCreationReady
            else -> SingleThreadCreated
        }
    }

    private suspend fun isFirstThread(): Boolean {
        val flow = threads.flow
        threads.refresh()
        val threadList = flow.first()
        val isFirst = threadList.isEmpty()
        if (!isFirst) selectedThreadRepository.chatThreadHandler = threads.thread(threadList.first())
        return isFirst
    }

    private suspend fun setCustomFields() {
        val customerCustomFields = chat.configuration.customerCustomFields
        val fields = valueStorage.getCustomerCustomValues().filterKeys(customerCustomFields::containsField)
        customerCustomFields.validate(fields)
        chat.customFields().add(fields)
    }

    internal fun setThreadName(threadName: String) {
        selectedThreadRepository.chatThreadHandler?.setName(threadName)
    }

    internal fun reportOnResume() {
        events.chatWindowOpen()
    }

    private fun showDialog(dialog: Dialogs) {
        showDialog.value = dialog
    }

    internal fun dismissDialog() {
        showDialog.value = None
    }

    internal fun editThreadName() {
        showDialog(EditThreadName)
    }

    internal fun confirmEditThreadName(name: String) {
        dismissDialog()

        setThreadName(name)
    }

    internal fun startEditingCustomValues() {
        showDialog(CustomValues)
    }

    internal fun confirmEditingCustomValues(values: CustomValueItemList) {
        dismissDialog()
        viewModelScope.launch(Dispatchers.Default) {
            selectedThreadRepository.chatThreadHandler?.customFields()?.add(values.extractStringValues())
        }
    }

    internal fun cancelEditingCustomValues() {
        dismissDialog()
    }

    internal fun showPreChatSurvey(survey: PreChatSurvey) {
        showDialog(Survey(survey))
    }

    internal fun reconnect() {
        chatProvider.reconnect()
    }

    /**
     * Definition of navigation states for the view model.
     */
    sealed interface NavigationState : State {
        /**
         * Navigation should be directed to multi-thread flow.
         */
        object MultiThreadEnabled : NavigationState

        /**
         * Navigation should be directed to single thread chat.
         */
        object SingleThreadCreated : NavigationState

        /**
         * Final state of navigation, either the activity has successfully navigated to [MultiThreadEnabled]
         * state or [SingleThreadCreated] state.
         */
        object NavigationFinished : NavigationState
    }

    /**
     * Definition of states for the view model.
     */
    sealed interface State {
        /**
         * Default state.
         */
        object Initial : State

        /**
         * Single thread is ready to be created.
         */
        object SingleThreadCreationReady : State

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
