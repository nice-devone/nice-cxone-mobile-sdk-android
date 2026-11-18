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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.launch

/**
 * Shared fixtures for the multi-thread [ChatThreadsHandler] test classes: the memoized [threads]
 * handler, the [config]/[prepare] overrides, the [threads] collector helper, and the list-and-message helpers. Each concern-focused
 * subclass extends this to inherit the connected multi-thread chat harness.
 */
internal abstract class AbstractChatThreadsHandlerTest : AbstractMultiThreadChatTest() {

    protected lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() = requireNotNull(super.config)

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    protected fun threads(listener: (List<ChatThread>) -> Unit): Cancellable {
        val job = testScope.launch { threads.threadsFlow.collect { listener(it) } }
        return Cancellable { job.cancel() }
    }

    /** Lists [thread], opens its handler so the cache watcher starts, then sends a MessageCreated for it. */
    protected fun collectAfterMessageCreated(
        thread: ChatThread,
        status: String = "new",
        handler: String? = null,
    ): List<List<ChatThread>> {
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        testScope.launch { threads.threadsFlow.collect { results.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(
                thread,
                makeMessageModel(threadIdOnExternalPlatform = thread.id),
                handler = handler,
                status = status,
            )
        )
        return results
    }

    /** What a refresh() brings: a new list without contact or case state, then the thread's metadata. */
    protected fun refetchThreadList(thread: ChatThread) {
        threads.refresh()
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        socketServer.sendServerMessage(
            ServerResponse.ThreadMetadataLoaded(agent = makeAgent(), message = makeMessageModel(threadIdOnExternalPlatform = thread.id))
        )
    }
}
