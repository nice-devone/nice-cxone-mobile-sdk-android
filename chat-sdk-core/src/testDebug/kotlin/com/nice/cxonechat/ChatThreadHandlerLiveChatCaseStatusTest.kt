/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Open
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Once a live chat case is closed, only an event for a new case may reopen the thread (web and iOS parity). */
internal class ChatThreadHandlerLiveChatCaseStatusTest : AbstractLiveChatTest() {

    private lateinit var thread: ChatThreadHandler
    private val emitted = mutableListOf<ChatThread>()

    override fun prepare() {
        super.prepare()
        connect()
        thread = chat.threads().thread(chat.threads().create().get())
        testScope.launch { thread.threadFlow.collect { emitted += it } }
    }

    @Test
    fun `stale MessageCreated on the closed case adds the message but keeps the thread closed`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        closeCase()

        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread.get(), message, status = "resolved"))

        assertTrue(thread.get().messages.any { it.id == message.idOnExternalPlatform }, "The message is still added")
        assertClosed()
    }

    @Test
    fun `stale duplicate MessageCreated on the closed case keeps the thread closed`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread.get(), message, status = "open"))
        closeCase()
        val emittedBefore = emitted.size

        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread.get(), message, status = "open"))

        assertClosed()
        assertEquals(emittedBefore, emitted.size, "A duplicate that changes nothing emits nothing")
    }

    @Test
    fun `stale non-closed CaseStatusChanged on the closed case keeps the thread closed`() {
        closeCase()

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread.get(), Open))

        assertClosed()
    }

    @Test
    fun `MessageCreated with an unknown closed case id keeps the thread closed`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        assertNull(thread.get().contactId, "Precondition: the thread has not learned its case id")
        closeCase(contactId = null)

        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread.get(), message, status = "new"))

        assertClosed()
    }

    @Test
    fun `stale CaseInboxAssigneeChanged on the closed case keeps the thread closed`() {
        closeCase()

        socketServer.sendServerMessage(ServerResponse.CaseInboxAssigneeChanged(thread.get(), makeAgent(), connection))
        assertClosed()

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread.get(), Open))
        assertClosed()
    }

    @Test
    fun `MessageCreated for a new case reopens the closed thread`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        closeCase()

        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(thread.get(), message, status = "new", contactId = NEW_CASE_ID)
        )

        assertEquals(ChatThreadState.Ready, thread.get().threadState)
        assertTrue(thread.get().canAddMoreMessages)
        assertEquals(NEW_CASE_ID, thread.get().contactId)
    }

    @Test
    fun `stale MessageCreated of a second closed case keeps the thread closed`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        closeCase()
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread.get(), Closed, contactId = NEW_CASE_ID))
        assertEquals(NEW_CASE_ID, thread.get().contactId, "The close records the case it closed")

        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(thread.get(), message, status = "open", contactId = NEW_CASE_ID)
        )

        assertClosed()
    }

    @Test
    fun `stale MessageCreated of the closed case with a closed status keeps the thread closed`() {
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.get().id)
        closeCase()

        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread.get(), message, status = "closed"))

        assertClosed()
    }

    @Test
    fun `non-closed CaseStatusChanged for a new case enables sending but leaves the thread closed`() {
        closeCase()

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread.get(), Open, contactId = NEW_CASE_ID))

        assertTrue(thread.get().canAddMoreMessages)
        assertEquals(ChatThreadState.Closed, thread.get().threadState, "Only MessageCreated or a recovery reopens it")
    }

    @Test
    fun `CaseInboxAssigneeChanged for a new case reopens the closed thread`() {
        closeCase()

        socketServer.sendServerMessage(
            ServerResponse.CaseInboxAssigneeChanged(thread.get(), makeAgent(), connection, contactId = NEW_CASE_ID)
        )

        assertEquals(ChatThreadState.Ready, thread.get().threadState)
        assertEquals(NEW_CASE_ID, thread.get().contactId)
    }

    private fun closeCase(contactId: String? = TestContactId) {
        assertTrue(thread.get().canAddMoreMessages, "Precondition: the thread is open")
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread.get(), Closed, contactId = contactId))
        assertClosed()
    }

    private fun assertClosed() {
        assertEquals(ChatThreadState.Closed, thread.get().threadState)
        assertFalse(thread.get().canAddMoreMessages)
    }

    private companion object {
        const val NEW_CASE_ID = "newCaseId"
    }
}
