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
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel responsible for providing [Flow] of [ChatState].
 */
@HiltViewModel
internal class ChatStateViewModel @Inject constructor(
    private val chatInstanceProvider: ChatInstanceProvider,
) : ViewModel(), ChatInstanceProvider.Listener {
    private val internalState: MutableStateFlow<ChatState> = MutableStateFlow(chatInstanceProvider.chatState)
    private val providerListener = object : ChatInstanceProvider.Listener {
        override fun onChatStateChanged(chatState: ChatState) {
            internalState.value = chatState
        }
    }.also(chatInstanceProvider::addListener)

    val state: StateFlow<ChatState> get() = internalState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        chatInstanceProvider.removeListener(providerListener)
    }
}
