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

package com.nice.cxonechat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import com.nice.cxonechat.ui.domain.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.domain.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.domain.model.threadOrAgentName
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.Initial
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.ThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.ThreadSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import java.util.UUID

@KoinViewModel
@Suppress(
    "TooManyFunctions"
)
internal class ChatThreadsViewModel(
    private val chat: Chat,
    private val selectedThreadRepository: SelectedThreadRepository,
) : ViewModel() {
    private val logger = LoggerScope(TAG, get(Logger::class.java, named(UiModule.loggerName)))
    private val uiCustomerFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Customer),
    )
    private val uiContactFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Contact),
    )
    private val threadsHandler = chat.threads()
    private val internalState: MutableStateFlow<State> = MutableStateFlow(Initial)
    val createThreadFailure = MutableStateFlow(null as Failure?)

    private val _refreshThreadName: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val refreshThreadName: StateFlow<Boolean> = _refreshThreadName.asStateFlow()

    private val threadFlow = threadsHandler.flow.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val nameOverrides = MutableStateFlow<Map<UUID, String>>(emptyMap())

    private val threadList: StateFlow<List<Thread>> = threadFlow
        .conflate()
        .map { chatThreads ->
            chatThreads
                .asSequence()
                .map { chatThread ->
                    val override = nameOverrides.value[chatThread.id]
                    toUiThread(chatThread).copy(name = override ?: chatThread.threadOrAgentName(isMultiThreadEnabled))
                }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val threadUpdates = MutableStateFlow<List<Thread>>(emptyList())

    private val isMultiThreadEnabled: Boolean = chat.configuration.hasMultipleThreadsPerEndUser

    /**
     * Updated state of the chat threads view.
     */
    val state
        get() = internalState
            .asStateFlow()

    val threads = merge(threadUpdates, threadList)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val backgroundThreadsFlow: SharedFlow<Thread?> = if (isMultiThreadEnabled) {
        threadList.subscribeToThreadUpdates()
            .updateLocalThreadCopy()
            .filter(::isThreadUpdated)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0) // Prevent re-emission of the same thread
    } else {
        MutableStateFlow(null).asSharedFlow()
    }

    private val showDialog = MutableStateFlow<Dialog>(Dialog.None)

    val dialogShown: StateFlow<Dialog> = showDialog.asStateFlow()

    sealed class Dialog {
        data object None : Dialog()
        data class EditThreadName(val thread: Thread) : Dialog()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<List<Thread>>.subscribeToThreadUpdates(): Flow<Thread> = flatMapLatest { threadList ->
        threadList.asFlow().flatMapMerge { thread ->
            threadsHandler.thread(thread.chatThread)
                .flow
                .map(thread::copy)
        }
    }

    private fun Flow<Thread>.updateLocalThreadCopy(): Flow<Thread> = onEach { thread ->
        threadUpdates.value = threadList.value.map { existingThread ->
            if (existingThread.id != thread.id) existingThread else thread
        }
    }

    private fun isThreadUpdated(chatThread: Thread): Boolean {
        val previousThreadsValue = threads.value
        val chatThreadHandler = selectedThreadRepository.chatThreadHandler
        if (
            chatThreadHandler === NoThreadHandler || // No thread is selected
            previousThreadsValue.isEmpty() // Previous threads state was not yet loaded - unable to compare
        ) {
            return false
        }
        // Flag to ignore updates from currently displayed thread
        val isThreadInBackground = chatThread.id != chatThreadHandler.get().id
        val previousThread = previousThreadsValue.firstOrNull { chatThread.id == it.id }
        // Flag to ignore metadata changes when threads are undergoing initial loading
        val previousThreadState = previousThread?.chatThread?.threadState ?: ChatThreadState.Pending
        val isPreviousThreadLoaded = ChatThreadState.Loaded.ordinal <= previousThreadState.ordinal
        // Flag indicating if there is a new message in the thread
        val lastMessageChanged = chatThread.messages.firstOrNull()?.id != previousThread?.messages?.firstOrNull()?.id
        // Flag indicating if the thread was be archived
        val archiveChanged = chatThread.chatThread.canAddMoreMessages != previousThread?.chatThread?.canAddMoreMessages
        return isThreadInBackground && isPreviousThreadLoaded && (lastMessageChanged || archiveChanged)
    }

    private fun toUiThread(chatThread: ChatThread) = Thread(
        chatThread = chatThread,
        name = chatThread.threadOrAgentName(isMultiThreadEnabled)
    )

    internal fun resetState() {
        internalState.value = Initial
    }

    internal fun resetCreateThreadState() {
        createThreadFailure.value = null
    }

    internal fun createThread() = logger.timedScope("createThread") {
        viewModelScope.launch {
            when (val preChatSurvey = threadsHandler.preChatSurvey) {
                null -> createThreadWorker(emptySequence())
                else -> internalState.value = ThreadPreChatSurveyRequired(preChatSurvey)
            }
        }
    }

    internal fun respondToSurvey(response: Sequence<PreChatResponse>) = logger.timedScope("respondToSurvey") {
        viewModelScope.launch {
            createThreadWorker(response)
        }
    }

    internal fun archiveThread(thread: Thread) = logger.timedScope("archiveThread(${thread.id})") {
        if (thread.chatThread.canAddMoreMessages &&
            // Verify that the latest version of the thread is not already archived
            threads.value.firstOrNull { it.id == thread.id }?.chatThread?.canAddMoreMessages == true
        ) {
            // Update the local copy of the thread to reflect that it can no longer accept new messages
            threadUpdates.value = threadList.value.map { existingThread: Thread ->
                if (existingThread.id != thread.id) {
                    existingThread
                } else {
                    existingThread.copy(chatThread = existingThread.chatThread.copy(canAddMoreMessages = false))
                }
            }
            threadsHandler.thread(thread.chatThread).archive()
        }
    }

    internal fun selectThread(thread: Thread) = logger.timedScope("selectThread(${thread.id})") {
        if (thread.id != selectedThreadRepository.chatThreadHandler.get().id) {
            selectedThreadRepository.chatThreadHandler = NoThreadHandler
        }
        selectedThreadRepository.chatThreadHandler = threadsHandler.thread(thread.chatThread)
        internalState.value = ThreadSelected
        nameOverrides.update { emptyMap() } // reset rename list
    }

    internal fun selectThreadById(threadId: UUID) {
        viewModelScope.launch(Dispatchers.Default) {
            logger.timedScope("selectThreadById($threadId)") {
                if (threadId == selectedThreadRepository.chatThreadHandler.get().id) {
                    return@timedScope
                }
                val flow = threadFlow
                refreshThreads()
                val threadList = flow.first()
                require(threadList.isNotEmpty())
                selectedThreadRepository.chatThreadHandler = threadsHandler.thread(threadList.first { it.id == threadId })
                internalState.value = ThreadSelected
            }
        }
    }

    private suspend fun createThreadWorker(response: Sequence<PreChatResponse>) =
        logger.timedScope("createThreadWorker") {
            withContext(Dispatchers.Default) {
                val result = runCatching {
                    chat.customFields().add(
                        withContext(Dispatchers.IO) {
                            uiCustomerFieldsProvider.customFields()
                        }
                    )
                    selectedThreadRepository.chatThreadHandler = threadsHandler.create(
                        customFields = withContext(Dispatchers.IO) {
                            uiContactFieldsProvider.customFields()
                        },
                        preChatSurveyResponse = response,
                    )
                }.foldToCreateThreadResult()

                if (result is Failure) {
                    createThreadFailure.value = result
                } else {
                    internalState.value = ThreadSelected
                }
            }
        }

    internal fun refreshThreads() = logger.timedScope("refreshThreads") {
        threadsHandler.refresh()
    }

    internal fun dismissDialog() {
        showDialog.value = Dialog.None
    }

    internal fun editThreadName(thread: Thread) {
        showDialog.value = Dialog.EditThreadName(thread)
    }

    internal fun confirmEditThreadName(thread: Thread, name: String) {
        threadsHandler.thread(thread.chatThread).setName(name)
        nameOverrides.update { it + (thread.id to name) }
        _refreshThreadName.update { !refreshThreadName.value }
    }

    /**
     * Definition of possible states for the chat threads view.
     */
    sealed interface State {
        /**
         * Default state, the thread list should be shown.
         */
        data object Initial : State

        /**
         * Possible state following [createThreadWorker] call.
         * This state carries information about the prechat survey which should be presented to the user.
         *
         * @property survey The [PreChatSurvey] which should be presented to user.
         */
        data class ThreadPreChatSurveyRequired(val survey: PreChatSurvey) : State

        /**
         * Indication that thread selection was performed.
         */
        data object ThreadSelected : State
    }

    companion object {
        const val TAG = "ChatThreadsViewModel"
    }
}
