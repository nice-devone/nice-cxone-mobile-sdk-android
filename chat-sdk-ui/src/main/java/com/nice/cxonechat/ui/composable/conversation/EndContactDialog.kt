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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.CLOSE_CHAT
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.NEW_CONVERSATION
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.SHOW_TRANSCRIPT
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel

@Composable
internal fun EndContactDialog(
    closeChat: () -> Unit,
    chatViewModel: ChatThreadViewModel,
    chatModel: ChatViewModel,
) {
    val agent = chatViewModel.assignedAgentFlow.collectAsState(initial = null)
    EndConversation(
        assignedAgent = agent,
        onDismiss = chatViewModel::dismissDialog,
        onUserSelection = {
            when (it) {
                SHOW_TRANSCRIPT -> {
                    // no-op required
                }

                NEW_CONVERSATION -> chatModel.refreshThreadState()
                CLOSE_CHAT -> closeChat()
            }
        }
    )
}
