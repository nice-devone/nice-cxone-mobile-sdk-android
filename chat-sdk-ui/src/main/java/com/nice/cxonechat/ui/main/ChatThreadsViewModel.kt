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

package com.nice.cxonechat.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatThreadsViewModel.State.Initial
import com.nice.cxonechat.ui.main.ChatThreadsViewModel.State.ThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.main.ChatThreadsViewModel.State.ThreadSelected
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.model.Thread
import com.nice.cxonechat.ui.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.getCustomerCustomValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get
import java.util.UUID

@KoinViewModel
@Suppress(
    "TooManyFunctions"
)
internal class ChatThreadsViewModel(
    private val chat: Chat,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val valueStorage: ValueStorage,
) : ViewModel() {
    private val logger = LoggerScope(TAG, get(Logger::class.java, named(UiModule.loggerName)))
    private val threadsHandler = chat.threads()
    private val internalState: MutableStateFlow<State> = MutableStateFlow(Initial)
    val createThreadFailure = MutableStateFlow(null as Failure?)

    private val threadFlow = threadsHandler.flow.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val threadList: StateFlow<List<Thread>> = threadFlow
        .conflate()
        .map { chatThreads ->
            chatThreads
                .asSequence()
                .map(::toUiThread)
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
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    } else {
        MutableStateFlow(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<List<Thread>>.subscribeToThreadUpdates(): Flow<Thread> = flatMapLatest { threadList ->
        threadList.asFlow().flatMapMerge(concurrency = 1) { thread ->
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
        val previousThreadsState = threads.value
        val chatThreadHandler = selectedThreadRepository.chatThreadHandler
        if (
            chatThreadHandler == null || // No thread is selected
            previousThreadsState.isEmpty() // Previous threads state was not yet loaded - unable to compare
        ) {
            return false
        }
        val isThreadInBackground = chatThread.id != chatThreadHandler.get().id
        val previousThread = previousThreadsState.firstOrNull { chatThread.id == it.id }
        val lastMessageChanged = chatThread.messages.firstOrNull()?.id != previousThread?.messages?.firstOrNull()?.id
        val archiveChanged = chatThread.chatThread.canAddMoreMessages != previousThread?.chatThread?.canAddMoreMessages
        return isThreadInBackground && (lastMessageChanged || archiveChanged)
    }

    private fun toUiThread(chatThread: ChatThread) = Thread(
        chatThread = chatThread,
        name = if (isMultiThreadEnabled) {
            chatThread.threadName.takeIf { !it.isNullOrBlank() } ?: "N/A"
        } else {
            chatThread.threadAgent?.fullName.orEmpty()
        }
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
        threadsHandler.thread(thread.chatThread).archive()
    }

    internal fun selectThread(thread: Thread) = logger.timedScope("selectThread(${thread.id})") {
        selectedThreadRepository.chatThreadHandler = threadsHandler.thread(thread.chatThread)
        internalState.value = ThreadSelected
    }

    internal fun selectThreadById(threadId: UUID) {
        viewModelScope.launch(Dispatchers.Default) {
            logger.timedScope("selectThreadById($threadId)") {
                if (threadId == selectedThreadRepository.chatThreadHandler?.get()?.id) {
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
                    chat.customFields().add(valueStorage.getCustomerCustomValues())
                    selectedThreadRepository.chatThreadHandler = threadsHandler.create(response)
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
