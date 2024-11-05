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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.model.CreateThreadResult.Success
import com.nice.cxonechat.ui.model.foldToCreateThreadResult
import com.nice.cxonechat.ui.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.getCustomerCustomValues
import com.nice.cxonechat.ui.util.Ignored
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@Suppress("TooManyFunctions")
@KoinViewModel
internal class ChatViewModel(
    private val valueStorage: ValueStorage,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chatProvider: ChatInstanceProvider,
    private val logger: Logger,
) : ViewModel(), LoggerScope by LoggerScope<ChatViewModel>(logger) {
    private val chat: Chat
        get() = requireNotNull(chatProvider.chat)

    private val threads by lazy { chat.threads() }

    private val events by lazy { chat.events() }

    sealed interface Dialogs {
        data object None : Dialogs
        data class Survey(val survey: PreChatSurvey) : Dialogs
        data class ThreadCreationFailed(val failure: Failure) : Dialogs
    }

    private val showDialog = MutableStateFlow<Dialogs>(None)
    val dialogShown = showDialog.asStateFlow()

    val chatMode: ChatMode
        get() = chat.chatMode

    val preChatSurvey
        get() = threads.preChatSurvey

    internal fun refreshThreadState() {
        viewModelScope.launch {
            resolveCurrentState()
        }
    }

    private fun createThread() {
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
                is Failure -> showDialog(Dialogs.ThreadCreationFailed(result))
                Success -> dismissDialog()
            }
        }
    }

    private suspend fun resolveCurrentState() {
        if (listOf(SingleThread, LiveChat).contains(chat.chatMode)) {
            when {
                selectFirstThread() -> Ignored
                else -> when (val chatSurvey = preChatSurvey) {
                    null -> createThread()
                    else -> showDialog(Survey(chatSurvey))
                }
            }
        }
    }

    /**
     * We're in single thread mode.  Select the first thread if it exists.
     *
     * Returns true iff the initial thread exists and was selected.
     */
    private suspend fun selectFirstThread(): Boolean {
        val flow = threads.flow
        threads.refresh()

        return flow.first()
            .filterNot { LiveChat === chatMode && !it.canAddMoreMessages }
            .firstOrNull()
            ?.let {
                selectedThreadRepository.chatThreadHandler = threads.thread(it)
                true
            }
            ?: false
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

    internal fun prepare(context: Context) {
        val chatState = chatProvider.chatState
        if (chatState === ChatState.Initial) {
            chatProvider.prepare(context)
        } else {
            warning("Chat already prepared, currentState: $chatState")
        }
    }

    internal fun connect() {
        val chatState = chatProvider.chatState
        if (chatState !== ChatState.Prepared && chatState !== ChatState.ConnectionLost && chatState !== ChatState.Offline) {
            error("Unable to connect chat in state: $chatState")
            return
        }
        chatProvider.connect()
    }

    internal fun close() {
        chatProvider.close()
    }
}
