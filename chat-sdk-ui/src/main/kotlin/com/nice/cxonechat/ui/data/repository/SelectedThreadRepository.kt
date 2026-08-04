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

package com.nice.cxonechat.ui.data.repository

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Session-aware holder of the currently selected thread.
 *
 * SDK handlers are scoped to a connected session (one [Chat] instance): [ChatInstanceProvider]
 * rebuilds the instance for each session, and a previous session's handler graph is dead. This
 * repository therefore re-derives the selected thread's handler from the fresh [Chat] on every
 * [ChatInstanceProvider.Listener.onChatChanged], keeping the thread identity but never serving a
 * stale handler — the derived instance re-keys every downstream collector, because the state flow
 * emits on reference inequality.
 */
@Single
internal class SelectedThreadRepository(
    chatProvider: ChatInstanceProvider,
) : ChatInstanceProvider.Listener {
    private val mutableChatThreadHandlerFlow: MutableStateFlow<ChatThreadHandler> = MutableStateFlow(NoThreadHandler)

    var chatThreadHandler: ChatThreadHandler
        get() = mutableChatThreadHandlerFlow.value
        set(value) {
            mutableChatThreadHandlerFlow.value = value
        }
    val chatThreadHandlerFlow = mutableChatThreadHandlerFlow.asStateFlow()

    init {
        // The repository is a Koin singleton, so this instance stays strongly reachable for the
        // whole process despite the provider's weak listener registration.
        chatProvider.addListener(this)
    }

    override fun onChatChanged(chat: Chat?) {
        val currentHandler = mutableChatThreadHandlerFlow.value
        if (currentHandler === NoThreadHandler) return
        mutableChatThreadHandlerFlow.value = when (chat) {
            null -> NoThreadHandler
            else -> chat.threads().thread(currentHandler.get())
        }
    }
}
