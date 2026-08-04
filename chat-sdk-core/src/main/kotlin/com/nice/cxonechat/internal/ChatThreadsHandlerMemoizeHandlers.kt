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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [ChatThreadsHandler] which prevents creation of multiple instances of
 * [ChatThreadHandler] for a [ChatThread] with the same [ChatThread.id].
 *
 * This is load-bearing, not incidental:
 * - Handler construction has real side effects (welcome-message preparation in
 *   [ChatThreadHandlerWelcome] / [ChatThreadHandlerWelcomeLiveChat], begin-conversation in
 *   [ChatThreadHandlerLiveChat]). [ChatThreadHandlerWelcomeLiveChat] in particular registers
 *   exclusive `addCallback` socket listeners in its `init`. Multiple independent instances for the
 *   same thread id risk duplicate welcome/begin-conversation handling. Removing this class was
 *   considered and explicitly rejected in DE-166650: it would require side-effect-free handler
 *   construction, but the eager welcome-message-as-first-message behavior is desired and was kept.
 * - [ChatThreadHandlerShared] (applied by [ChatThreadsHandlerImpl.thread]) shares `threadFlow`
 *   across every collector of a given handler instance so concurrent collectors (e.g. a thread-list
 *   background subscription and an open conversation for the same thread) don't independently race
 *   each other's event handling. That sharing only has an effect because this class guarantees they
 *   all receive the *same* instance; without it, each caller would get its own handler with its own
 *   independent state, silently defeating the sharing.
 */
internal class ChatThreadsHandlerMemoizeHandlers(
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    private val threadHandlersMemoized = ConcurrentHashMap<UUID, ChatThreadHandler>()

    // Mirror the eviction logic from ChatThreadsHandlerMessages.threadsFlow: when a thread
    // disappears from the server list its memoized handler must be cleared so that the next
    // thread() call reaches ChatThreadsHandlerMessages and reinstalls the cache-watcher job.
    override val threadsFlow: Flow<List<ChatThread>> = origin.threadsFlow
        .onEach { threads ->
            threadHandlersMemoized.keys.retainAll(
                threads.mapTo(HashSet(), ChatThread::id)
            )
        }

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = origin.create(customFields, preChatSurveyResponse).also(::memoizeThreadHandler)

    override fun thread(thread: ChatThread): ChatThreadHandler =
        threadHandlersMemoized.computeIfAbsent(thread.id) { origin.thread(thread) }

    private fun memoizeThreadHandler(threadHandler: ChatThreadHandler) {
        threadHandlersMemoized[threadHandler.get().id] = threadHandler
    }
}
