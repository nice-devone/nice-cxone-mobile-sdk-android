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

import com.nice.cxonechat.internal.ChatThreadHandlerFilter
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageContent
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadHandlerFilterTest : AbstractMultiThreadChatTest() {
    private lateinit var origin: ChatThreadHandler
    private lateinit var filter: ChatThreadHandlerFilter
    private lateinit var chatThread: ChatThread
    private lateinit var unsupportedMessage: com.nice.cxonechat.message.Message
    private lateinit var supportedMessage: com.nice.cxonechat.message.Message
    private lateinit var originFlow: MutableSharedFlow<ChatThread>

    override fun prepare() {
        super.prepare()
        val threadId = UUID.randomUUID()
        unsupportedMessage = makeMessage(
            model = makeMessageModel(
                threadIdOnExternalPlatform = threadId,
                messageContent = makeMessageContent().copy(
                    parameters = Parameters.Object(isUnsupportedMessageTypeAnswer = true)
                ),
            )
        )
        supportedMessage = makeMessage(
            model = makeMessageModel(threadIdOnExternalPlatform = threadId)
        )
        chatThread = makeChatThread(messages = listOf(supportedMessage, unsupportedMessage), id = threadId)
        originFlow = MutableSharedFlow(extraBufferCapacity = 1)
        origin = mockk(relaxed = true)
        every { origin.get() } returns chatThread
        every { origin.threadFlow } returns originFlow
        filter = ChatThreadHandlerFilter(origin, mockk<ChatWithParameters>(relaxed = true))
    }

    @Test
    fun onUpdated_filters_thread_for_listener() {
        var listenerCalledWith: ChatThread? = null
        val job = testScope.launch {
            filter.threadFlow.collect { listenerCalledWith = it }
        }
        originFlow.tryEmit(chatThread)
        testScope.advanceUntilIdle()
        job.cancel()

        val actual = listenerCalledWith
        assertNotNull(actual)
        assertEquals(1, actual.messages.size)
        assertEquals(supportedMessage, actual.messages[0])
    }

    @Test
    fun filter_removes_unsupported_messages() {
        val filtered = filter.get()
        assertEquals(1, filtered.messages.size)
        assertEquals(supportedMessage, filtered.messages[0])
    }
}
