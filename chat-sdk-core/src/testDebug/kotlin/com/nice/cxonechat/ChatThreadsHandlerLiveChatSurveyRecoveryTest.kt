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

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.SatisfactionSurvey
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Closed
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A Satisfaction Survey missed while the app was closed must not be discarded by
 * [ChatThreadsHandlerLive]'s usual "closed thread recovered -> auto-create a fresh thread" path.
 */
internal class ChatThreadsHandlerLiveChatSurveyRecoveryTest : AbstractLiveChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    @Test
    fun `threadsFlow keeps the closed thread when recovery carries an undelivered satisfaction survey`() {
        connect()
        val closedThread = makeChatThread(threadState = Closed, canAddMoreMessages = false)

        val actual = testCallback(::threads) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = closedThread,
                    messages = arrayOf(SatisfactionSurvey(closedThread.id)),
                )
            )
        }

        assertEquals(1, actual.size)
        assertEquals(closedThread.id, actual.first().id, "Must keep the original closed thread, not replace it")
        assertEquals(false, actual.first().canAddMoreMessages)
    }

    @Test
    fun `threadsFlow creates a new thread when the closed thread's survey was already delivered`() {
        connect()
        val closedThread = makeChatThread(threadState = Closed, canAddMoreMessages = false)
        storage.markSurveyDeliveredIfAbsent(closedThread.id)

        val actual = testCallback(::threads) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = closedThread,
                    messages = arrayOf(SatisfactionSurvey(closedThread.id)),
                )
            )
        }

        assertEquals(1, actual.size)
        assertNotEquals(closedThread.id, actual.first().id, "A survey already shown must not block a fresh thread")
        assertEquals(true, actual.first().canAddMoreMessages)
    }

    @Test
    fun `threadsFlow creates a new thread when the closed thread carries no survey`() {
        connect()
        val closedThread = makeChatThread(threadState = Closed, canAddMoreMessages = false)

        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.LivechatRecovered(thread = closedThread))
        }

        assertEquals(1, actual.size)
        assertNotEquals(closedThread.id, actual.first().id, "No survey pending - today's auto-create behavior must be unchanged")
        assertEquals(true, actual.first().canAddMoreMessages)
    }

    @Test
    fun `threadsFlow creates a new thread instead of dropping recovery when the survey popup is malformed`() {
        connect()
        val closedThread = makeChatThread(threadState = Closed, canAddMoreMessages = false)

        val actual = testCallback(::threads) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = closedThread,
                    messages = arrayOf(SatisfactionSurvey(closedThread.id, surveyUrl = "not-a-valid-url")),
                )
            )
        }

        assertEquals(1, actual.size, "A malformed popup must be dropped, not crash the whole recovery result")
        assertNotEquals(closedThread.id, actual.first().id, "Treated like no survey pending - a fresh thread is created")
        assertEquals(true, actual.first().canAddMoreMessages)
    }

    @Test
    fun `threadsFlow never reports a kept closed thread as still writable`() {
        connect()
        // Inconsistent server data: contact status is Closed, but the raw thread payload still
        // carries canAddMoreMessages = true - the kept thread must not inherit that writable flag.
        val closedThread = makeChatThread(threadState = Closed, canAddMoreMessages = true)

        val actual = testCallback(::threads) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = closedThread,
                    messages = arrayOf(SatisfactionSurvey(closedThread.id)),
                    status = ContactStatus.Closed,
                )
            )
        }

        assertEquals(1, actual.size)
        assertEquals(closedThread.id, actual.first().id, "Must still keep the thread - its survey is undelivered")
        assertEquals(false, actual.first().canAddMoreMessages, "A kept closed thread must never be reported as writable")
    }

    // --

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable {
        val job = testScope.launch { threads.threadsFlow.collect { listener(it) } }
        return Cancellable { job.cancel() }
    }
}
