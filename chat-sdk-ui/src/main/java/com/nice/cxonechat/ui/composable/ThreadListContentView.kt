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

package com.nice.cxonechat.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nice.cxonechat.ui.main.ChatThreadsViewModel

@Composable
internal fun ThreadListContentView(chatThreadsViewModel: ChatThreadsViewModel, onThreadSelected: () -> Unit) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState === State.RESUMED) chatThreadsViewModel.refreshThreads()
    }
    val chatThreadsState by chatThreadsViewModel.state.collectAsState()
    LaunchedEffect(chatThreadsState) {
        if (chatThreadsState === ChatThreadsViewModel.State.ThreadSelected) {
            onThreadSelected()
        }
    }
    MultiThreadContent(
        state = chatThreadsState,
        threads = chatThreadsViewModel.threads.collectAsState().value,
        threadFailure = chatThreadsViewModel.createThreadFailure.collectAsState().value,
        onThreadSelected = chatThreadsViewModel::selectThread,
        onArchiveThread = chatThreadsViewModel::archiveThread,
        resetState = chatThreadsViewModel::resetState,
        respondToSurvey = chatThreadsViewModel::respondToSurvey,
        resetCreateThreadState = chatThreadsViewModel::resetCreateThreadState,
    )
}
