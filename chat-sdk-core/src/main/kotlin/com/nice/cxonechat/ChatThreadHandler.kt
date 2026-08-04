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

package com.nice.cxonechat

import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.flow.Flow

/**
 * Instance of a thread handler. This instance will contain the most up-to-date
 * [thread][ChatThread], even newer than the thread it was created from.
 *
 * This object will not transfer any changes to the parent object and all updates
 * are contained within this class. It's up to the client to keep the instance
 * alive as long as they need it.
 *
 * Creating the handler may trigger mode- and thread-state-dependent background work — for example
 * welcome-message handling, and in live-chat configurations thread recovery and begin-conversation —
 * so construction is not guaranteed to be free of side effects. The most visible case for integrators
 * is the live-chat welcome message, delivered eagerly as the thread's first message at most once per
 * thread; see [ChatThreadsHandler.thread].
 */
@Public
@Suppress(
    "ComplexInterface",
)
interface ChatThreadHandler {

    /**
     * A [Flow] of [ChatThread] updates for this handler.
     * Emits the current thread state and subsequent updates triggered by
     * thread recovered events (triggered by [refresh]), message events
     * (triggered by [messages] and its methods), and metadata events.
     *
     * The flow replays the latest thread state when collected. It is safe to collect from
     * multiple places at once (e.g. a thread list and an open conversation for the same thread) —
     * all collectors observe the same sequence of updates, the same as [ChatThreadsHandler.threadsFlow].
     *
     * As with [ChatThreadsHandler.threadsFlow], this handler's updates are tied to the chat session
     * that created it: once the underlying connection is closed, this instance stops updating.
     * Calling [ChatThreadsHandler.thread] again on a [ChatThreadsHandler] obtained from that same,
     * now-closed session returns the same stale instance — obtain a handler from a freshly connected
     * [Chat] (e.g. via [ChatInstanceProvider], which rebuilds the session on reconnect) instead.
     *
     * @see get
     */
    val threadFlow: Flow<ChatThread>

    /**
     * Returns the latest [thread][ChatThread] snapshot observed on this handler.
     * This value is safe to call on the main thread.
     *
     * State only starts advancing once [threadFlow] has been collected at least once; before
     * that, [get] alone will not reflect server-side updates. Once started, updates continue
     * to be applied for the remainder of the chat session, even if no collector remains active.
     * Use [threadFlow] to observe changes reactively.
     */
    fun get(): ChatThread

    /**
     * Notifies the server asynchronously that client wants to change the thread
     * name to the supplied [name]. The change can fail, though if it doesn't [get]
     * methods will return updated thread.
     */
    fun setName(name: String)

    /**
     * Notifies the server asynchronously that client wants the thread to be
     * refreshed. The request can be performed even if the client doesn't
     * expect new data. In which case the [get] returns updated value anyway.
     *
     * Note: if the thread is in [ChatThreadState.Pending] state (not yet
     * recovered from the server), this call is silently ignored.
     */
    fun refresh()

    /**
     * Requests that the server archive this thread.
     *
     * @return `true` if the thread was successfully archived, `false` otherwise.
     */
    suspend fun archive(): Boolean

    /**
     * Returns new instance of message handler for this [ChatThread].
     * @see ChatThreadMessageHandler
     */
    fun messages(): ChatThreadMessageHandler

    /**
     * Returns new instance of thread event handler for this [ChatThread].
     * @see ChatThreadEventHandler
     */
    fun events(): ChatThreadEventHandler

    /**
     * Return new instance of thread action handler for this [ChatThread].
     * @see ChatThreadActionHandler
     */
    fun actions(): ChatThreadActionHandler

    /**
     * Returns new instance of field handler for this [ChatThread].
     * @see ChatFieldHandler
     */
    fun customFields(): ChatFieldHandler

    /**
     * Terminate the contact.
     *
     * @throws InvalidStateException if the current channel is not a live chat,
     * or if the [ChatThread.threadState] isn't in state [ChatThreadState.Ready],
     * or [ChatThreadState.Closed].
     *
     */
    fun endContact()
}
