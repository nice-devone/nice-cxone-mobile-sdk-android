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
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * ChatThreadHandler that shares [threadFlow] across all collectors.
 *
 * [origin]'s decorator chain mutates a shared, unsynchronized `ChatThreadMutable` on every event;
 * as a cold flow it would re-run that mutation independently per collector. Since
 * [ChatThreadsHandlerMemoizeHandlers] hands the same handler instance to every caller for a given
 * thread id, more than one collector attaching at once (e.g. a list-screen background subscription
 * and a conversation-screen subscription for the same thread) is the normal case, not an edge case.
 * Sharing ensures the mutation runs exactly once per event and every collector observes the same
 * result, instead of racing on who mutates first and silently dropping the update for the loser.
 *
 * Uses [SharingStarted.Eagerly] in the chat session's scope: the same session-scoped subscription
 * [ChatThreadsHandlerImpl.threadsFlow] relies on, with the same trade-off — this flow goes stale after a
 * `close()` + `connect()` cycle on the *same* `Chat` instance (the session-scoped subscription is
 * cancelled and never restarts). This is a non-issue in practice because production always rebuilds
 * the `Chat` instance on reconnect (`ChatInstanceProvider`), which yields a fresh handler graph rather
 * than reviving this one.
 *
 * Must be the outermost decorator: everything below it must have exactly one collector (the shared
 * flow itself) so its own internal event handling isn't subject to the same race this class fixes.
 *
 * Why [SharingStarted.Eagerly]: the chain below applies socket events (a new message, a recovery) to
 * the thread only while it is collected, and those events are not replayed. Started lazily, an event
 * that arrived before the first collector was lost for good, from [ChatThreadHandler.get] too. Started
 * eagerly, the chain subscribes as soon as the session's dispatcher runs the sharing coroutine after
 * the handler is built, and keeps running for the rest of the chat session, like a lazily started one
 * after its first collector. [ChatThreadsHandlerMemoizeHandlers] keeps that to one chain per thread.
 *
 * @param origin The original decorated `ChatThreadHandler` instance.
 * @param coroutineScope The chat session's own scope, so a closed/rebuilt session doesn't try to
 * revive this subscription (mirrors [ChatThreadsHandlerImpl.threadsFlow], [ChatThreadHandlerLiveChat]'s
 * `eagerLiveChatRecovery`, and [ChatThreadsHandlerMessages]'s cache watcher).
 */
internal class ChatThreadHandlerShared(
    private val origin: ChatThreadHandler,
    coroutineScope: CoroutineScope,
) : ChatThreadHandler by origin {

    override val threadFlow: Flow<ChatThread> = origin.threadFlow
        .shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)
}
