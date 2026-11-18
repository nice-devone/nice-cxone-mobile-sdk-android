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

import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Open
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Received
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ChatThreadsHandlerLiveChatTest : AbstractLiveChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    @Test
    fun `threads notifies case closed`() {
        connect()
        val initial = makeChatThread(threadState = Received)
        val expected = initial.copy(
            canAddMoreMessages = false,
            threadState = ChatThreadState.Closed,
            contactId = TestContactId,
        )
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.LivechatRecovered(thread = initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected, Closed))
        }
        assertNotEquals(expected, initial)
        assertEquals(1, actual.size)
        assertEquals(expected, actual.first().asCopyable().copy())
    }

    @Test
    fun `threadsFlow triggers only one recover request with multiple concurrent collectors`() {
        connect()
        assertSendTexts(ServerRequest.RecoverLiveChatThread(connection, null)) {
            val job1 = testScope.launch { threads.threadsFlow.collect {} }
            val job2 = testScope.launch { threads.threadsFlow.collect {} }
            job1.cancel()
            job2.cancel()
        }
    }

    @Test
    fun `threadsFlow emits recovered thread when livechat recovered`() {
        connect()
        val thread = makeChatThread(threadState = Received)
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.LivechatRecovered(thread = thread))
        }
        assertEquals(1, actual.size)
        assertEquals(thread.id, actual.first().id)
    }

    @Test
    fun `threadsFlow removes begin conversation message when livechat is recovered`() {
        connect()
        val thread = makeChatThread(threadState = Received)
        val beginMessage = makeMessageModel(
            threadIdOnExternalPlatform = thread.id,
            messageContent = MessagePolyContent.Text(
                payload = MessagePolyContent.Text.Payload(BEGIN_CONVERSATION_MESSAGE),
                parameters = Parameters.Object(isInitialMessage = true),
            ),
        )
        val expectedText = "Hello from agent"
        val regularMessage = makeMessageModel(
            threadIdOnExternalPlatform = thread.id,
            messageContent = MessagePolyContent.Text(
                payload = MessagePolyContent.Text.Payload(expectedText),
            ),
        )

        val actual = testCallback(::threads) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = thread,
                    messages = arrayOf(beginMessage, regularMessage),
                )
            )
        }

        val emitted = actual.single()
        assertEquals(listOf(expectedText), emitted.messages.filterIsInstance<Text>().map(Text::text))
        assertEquals(listOf(expectedText), emitted.messages.map { (it as Text).text })
    }

    @Test
    fun `threadsFlow late subscriber after livechat recovery receives authoritative thread from replay not emptyList`() {
        connect()
        val thread = makeChatThread(threadState = Received)
        val firstJob = testScope.launch { threads.threadsFlow.collect {} }
        socketServer.sendServerMessage(ServerResponse.LivechatRecovered(thread = thread))
        firstJob.cancel()

        val lateResults = mutableListOf<List<ChatThread>>()
        val lateJob = testScope.launch { threads.threadsFlow.take(1).toList(lateResults) }
        assertEquals(1, lateResults.size, "Late subscriber should receive one emission from replay")
        assertEquals(1, lateResults[0].size, "Replay must hold the authoritative thread, not an empty list")
        assertEquals(thread.id, lateResults[0][0].id)
        lateJob.cancel()
    }

    @Test
    fun `threadsFlow does not republish a stale thread copy on a handler-only case status change`() {
        connect()
        val created = threads.create().get()
        val emissions = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { emissions += it } }
        val before = emissions.size

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(created, Open, handler = "cognigy"))
        job.cancel()

        assertEquals(emptyList(), emissions.drop(before))
    }

    // --

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable {
        val job = testScope.launch { threads.threadsFlow.collect { listener(it) } }
        return Cancellable { job.cancel() }
    }
}
