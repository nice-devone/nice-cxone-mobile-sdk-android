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
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Pending
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsHandlerReplayLastEmptyTest {

    private val threadsSource = MutableSharedFlow<List<ChatThread>>(replay = 1)
    private val mockOrigin: ChatThreadsHandler = mockk(relaxed = true) {
        every { threadsFlow } returns threadsSource
    }
    private val handler = ChatThreadsHandlerReplayLastEmpty(mockOrigin)

    @Test
    fun `threadsFlow emits passthrough when no latest thread`() = runTest(UnconfinedTestDispatcher()) {
        val threads = listOf(makeChatThread())
        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }
        threadsSource.emit(threads)
        job.cancel()
        assertEquals(1, results.size)
        assertEquals(threads, results[0])
    }

    @Test
    fun `threadsFlow prepends latest thread when not yet in server list`() = runTest(UnconfinedTestDispatcher()) {
        val created = makeChatThread(threadState = Pending)
        val mockHandler = mockk<ChatThreadHandler> { every { get() } returns created }
        every { mockOrigin.create(any(), any()) } returns mockHandler
        handler.create()

        val serverThreads = listOf(makeChatThread())
        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }
        threadsSource.emit(serverThreads)
        job.cancel()
        assertEquals(1, results.size)
        assertEquals(2, results[0].size)
        assertEquals(created.id, results[0].first().id)
    }

    @Test
    fun `threadsFlow clears latest thread once server confirms thread`() = runTest(UnconfinedTestDispatcher()) {
        val created = makeChatThread(threadState = Pending)
        val mockHandler = mockk<ChatThreadHandler> { every { get() } returns created }
        every { mockOrigin.create(any(), any()) } returns mockHandler
        handler.create()

        val otherThread = makeChatThread()
        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }

        // Emission 1: thread not in server list yet — should prepend latestThread
        threadsSource.emit(listOf(otherThread))
        // Emission 2: server list includes the created thread — clears latestThread, emits as-is
        threadsSource.emit(listOf(created, otherThread))
        // Emission 3: latestThread is now null — passthrough
        threadsSource.emit(listOf(makeChatThread(), makeChatThread(), makeChatThread()))
        job.cancel()

        assertEquals(3, results.size)
        assertEquals(2, results[0].size, "Emission 1: latestThread prepended")
        assertEquals(created.id, results[0].first().id)
        assertEquals(2, results[1].size, "Emission 2: server confirmed, emit as-is")
        assertEquals(3, results[2].size, "Emission 3: latestThread cleared, passthrough")
    }

    @Test
    fun `threadsFlow prepends latest thread when created via custom fields`() = runTest(UnconfinedTestDispatcher()) {
        val created = makeChatThread(threadState = Pending)
        val mockHandler = mockk<ChatThreadHandler> { every { get() } returns created }
        every { mockOrigin.create(any(), any()) } returns mockHandler
        handler.create(customFields = emptyMap())

        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }
        threadsSource.emit(listOf(makeChatThread()))
        job.cancel()
        assertEquals(2, results[0].size)
        assertEquals(created.id, results[0].first().id)
    }

    @Test
    fun `threadsFlow prepends latest thread when created via pre-chat survey response`() = runTest(UnconfinedTestDispatcher()) {
        val created = makeChatThread(threadState = Pending)
        val mockHandler = mockk<ChatThreadHandler> { every { get() } returns created }
        every { mockOrigin.create(any(), any()) } returns mockHandler
        handler.create(preChatSurveyResponse = emptySequence<PreChatSurveyResponse<out FieldDefinition, out Any>>())

        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }
        threadsSource.emit(listOf(makeChatThread()))
        job.cancel()
        assertEquals(2, results[0].size)
        assertEquals(created.id, results[0].first().id)
    }

    @Test
    fun `threadsFlow prepends latest thread when created via custom fields and pre-chat survey response`() = runTest(UnconfinedTestDispatcher()) {
        val created = makeChatThread(threadState = Pending)
        val mockHandler = mockk<ChatThreadHandler> { every { get() } returns created }
        every { mockOrigin.create(any(), any()) } returns mockHandler
        handler.create(
            customFields = emptyMap(),
            preChatSurveyResponse = emptySequence<PreChatSurveyResponse<out FieldDefinition, out Any>>(),
        )

        val results = mutableListOf<List<ChatThread>>()
        val job = launch { handler.threadsFlow.collect { results.add(it) } }
        threadsSource.emit(listOf(makeChatThread()))
        job.cancel()
        assertEquals(2, results[0].size)
        assertEquals(created.id, results[0].first().id)
    }
}
