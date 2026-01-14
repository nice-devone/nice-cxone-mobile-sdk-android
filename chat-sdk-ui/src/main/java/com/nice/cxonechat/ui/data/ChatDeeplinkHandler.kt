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

package com.nice.cxonechat.ui.data

import android.content.Intent
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.parseThreadDeeplink
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scope(ChatActivity::class)
@Scoped
internal class ChatDeeplinkHandler(
    private val chatStateViewModel: ChatStateViewModel,
    private val chatThreadsViewModel: Lazy<ChatThreadsViewModel>,
    @InjectedParam private val loggerScope: LoggerScope,
) {
    suspend fun handleDeeplink(intent: Intent) {
        val data = intent.data ?: return
        withContext(Dispatchers.Default) {
            // Wait until chat is in a state capable of handling deeplink
            val filteredState = chatStateViewModel.state.filter { state ->
                state === ChatState.Ready || // Ready to handle deeplink
                        state === ChatState.SdkNotSupported // Terminal state
            }.first()
            if (filteredState != ChatState.Ready) {
                loggerScope.warning("Chat is not ready, cannot handle deeplink: $data")
                return@withContext
            }
            data
                .parseThreadDeeplink()
                .mapCatching { chatThreadsViewModel.value.selectThreadById(it) }
                .onFailure {
                    loggerScope.warning("Failed to parse deeplink: $data", it)
                }
        }
    }
}
