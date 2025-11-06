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
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.ui.data.ChatErrorState
import com.nice.cxonechat.ui.util.ErrorGroup
import com.nice.cxonechat.ui.util.ErrorGroup.DO_NOTHING
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel responsible for providing [Flow] of [ChatState].
 */
@KoinViewModel
internal class ChatStateViewModel(
    private val chatInstanceProvider: ChatInstanceProvider,
) : ViewModel() {
    private val internalState: MutableStateFlow<ChatState> = MutableStateFlow(chatInstanceProvider.chatState)
    private val providerListener = object : ChatInstanceProvider.Listener {
        override fun onChatStateChanged(chatState: ChatState) {
            internalState.value = chatState
        }
        override fun onChatRuntimeException(exception: RuntimeChatException) {
            handleChatRuntimeException(exception)
        }
    }.also(chatInstanceProvider::addListener)

    private val noError = ChatErrorState(DO_NOTHING, null)
    private val _chatErrorState: MutableStateFlow<ChatErrorState> = MutableStateFlow(noError)
    val chatErrorState: StateFlow<ChatErrorState> = _chatErrorState.asStateFlow()

    val state: StateFlow<ChatState> get() = internalState.asStateFlow()

    private fun handleChatRuntimeException(exception: RuntimeChatException) {
        val errorGroup = when (exception) {
            is RuntimeChatException.AuthorizationError -> ErrorGroup.HIGH
            is RuntimeChatException.ServerCommunicationError -> ErrorGroup.LOW
            else -> DO_NOTHING
        }
        _chatErrorState.value = ChatErrorState(errorGroup, exception.message)
    }

    internal fun resetError() {
        _chatErrorState.value = noError
    }

    internal fun showError(errorGroup: ErrorGroup, message: String, title: String? = null) {
        _chatErrorState.value = ChatErrorState(errorGroup, message, title)
    }

    override fun onCleared() {
        super.onCleared()
        chatInstanceProvider.removeListener(providerListener)
        internalState.value = ChatState.Initial
    }
}
