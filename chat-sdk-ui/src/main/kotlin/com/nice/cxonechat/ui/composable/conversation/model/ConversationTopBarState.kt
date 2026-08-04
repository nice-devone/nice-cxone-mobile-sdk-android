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

package com.nice.cxonechat.ui.composable.conversation.model

import androidx.compose.runtime.Stable
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Stable
internal data class ConversationTopBarState(
    val threadName: Flow<String?>,
    val isMultiThreaded: Boolean,
    val hasQuestions: Boolean,
    val isLiveChat: Boolean,
    val liveChatAllowTranscript: Boolean,
    val isArchived: StateFlow<Boolean>,
    val threadState: StateFlow<ChatThreadState>,
    val agentName: Flow<String?> = flowOf(null),
    val hasAiMessages: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow(),
    // Production code overrides hasAgent with hasAgentFlow from the ViewModel, which derives from
    // the assigned Agent object (presence), not agentName (name visibility). With PII hiding active,
    // an agent can be present but have a null name — the default here is only valid for tests/previews
    // that never need to distinguish those two states.
    val hasAgent: Flow<Boolean> = agentName.map { it != null },
)
