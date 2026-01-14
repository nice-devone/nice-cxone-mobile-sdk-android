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

package com.nice.cxonechat.ui.composable

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nice.cxonechat.ui.composable.conversation.ChatThreadTopBar
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadViewTopBar(isMultiThread: Boolean, isLiveChat: Boolean, scrollBehavior: TopAppBarScrollBehavior?) {
    val chatThreadViewModel = koinActivityViewModel<ChatThreadViewModel>()
    val threadNameFlow = remember { chatThreadViewModel.threadNameFlow }
    val hasQuestions = remember { chatThreadViewModel.hasQuestions }
    ChatThreadTopBar(
        scrollBehavior = scrollBehavior,
        conversationState = ConversationTopBarState(
            threadName = threadNameFlow,
            isMultiThreaded = isMultiThread,
            isLiveChat = isLiveChat,
            hasQuestions = hasQuestions,
            isArchived = chatThreadViewModel.isArchived,
            threadState = chatThreadViewModel.threadStateFlow,
        ),
        onEditThreadName = { chatThreadViewModel.editThreadName() },
        onEditThreadValues = chatThreadViewModel::startEditingCustomValues,
        onEndContact = chatThreadViewModel::endContact,
        displayEndConversation = chatThreadViewModel::showEndContactDialog,
        navigationIcon = {
            val dispatcher = (LocalActivity.current as? ComponentActivity)?.onBackPressedDispatcher
            val onBack: () -> Unit = {
                dispatcher?.onBackPressed()
            }
            BackButton(onBack)
        }
    )
}
