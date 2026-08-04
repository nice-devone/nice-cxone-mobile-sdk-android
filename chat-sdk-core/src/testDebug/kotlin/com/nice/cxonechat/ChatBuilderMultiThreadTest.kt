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

import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for [ChatBuilder] connect behavior specific to MultiThread mode.
 * In MultiThread mode, after authorization the SDK sends [ServerRequest.FetchThreadList]
 * to populate the thread list eagerly.
 */
internal class ChatBuilderMultiThreadTest : AbstractChatTestSubstrate() {

    // Default config already has hasMultipleThreadsPerEndUser = true (MultiThread mode).
    override fun prepare() = Unit

    @Test
    fun `connect sends FetchThreadList`() = runTest(dispatcher) {
        val code = "code"
        val verifier = "verifier"
        val (connection, builder) = prepareBuilder()
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
        ) {
            connect(builder) {
                every { storage.authToken } returns null
                every { storage.customerId } returns null
                setAuthorization(Authorization(code, verifier))
            }
        }
        testScheduler.advanceUntilIdle() // Await for background tasks to finish before verification
        verify(exactly = 1) {
            service.createOrUpdateVisitor(any(), any(), any())
        }
    }

    // ---

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }

    private fun connect(
        builder: ChatBuilder = prepareBuilder().second,
        consumerAuthorized: String = ServerResponse.ConsumerAuthorized(
            firstName = SocketFactoryMock.firstName,
            lastName = SocketFactoryMock.lastName,
        ),
        body: ChatBuilder.() -> ChatBuilder = { this },
    ): Chat = kotlinx.coroutines.runBlocking {
        val chat = builder
            .setDevelopmentMode(true)
            .body()
            .build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(consumerAuthorized)
        connectJob.join()
        chat
    }
}
