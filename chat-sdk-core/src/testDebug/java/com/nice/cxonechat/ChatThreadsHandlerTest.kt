/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Received
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class ChatThreadsHandlerTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() {
            return requireNotNull(super.config).copy(
                contactCustomFields = listOf(
                    Text("testField", "first field"),
                    Text("testField2", "first field")
                )
            )
        }

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test
    fun refresh_sendsExpectedMessage() {
        assertSendText(ServerRequest.FetchThreadList(connection)) {
            threads.refresh()
        }
    }

    @Test
    fun threads_notifies_withInitialList() {
        val initial = List(2) { makeChatThread(threadState = Received) }
        val message = makeMessageModel(threadIdOnExternalPlatform = initial[0].id)
        val agentModel = makeAgent()
        val expected = listOf(
            initial[0].copy(threadAgent = agentModel.toAgent(), messages = listOfNotNull(message.toMessage()), threadState = Loaded),
            initial[1]
        ).map {
            it.asMutable()
        }
        connect()
        // verify that the metadata is loaded when the list is received
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, initial[0]),
            ServerRequest.LoadThreadMetadata(connection, initial[1])
        ) {
            // Multithread threads should start in READY state.
            assertEquals(Ready, chatStateListener.connection)
            val actual = testCallback(::threads) {
                sendServerMessage(ServerResponse.ThreadListFetched(initial))
                sendServerMessage(
                    ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message)
                )
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun create_passesNewThread() {
        val handler = chat.threads().create()
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_withCustomParameters_passesNewThread() {
        val handler = chat.threads().create(emptyMap())
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun threads_notifies_caseClosed() {
        val initial = List(2) { makeChatThread(threadState = Received) }
        val expected = initial.toMutableList().also {
            it[0] = it[0].copy(canAddMoreMessages = false)
        }
        assertEquals(true, initial[0].canAddMoreMessages)
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.ThreadListFetched(initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected[0], Closed))
        }
        assertNotEquals(expected, initial)
        assertEquals(expected, actual)
    }

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable =
        threads.threads(listener = { listener(it) })
}
