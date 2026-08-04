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

import com.nice.cxonechat.ChatStateEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AwaitSocketTest {

    private val mockSocket = mockk<WebSocket>(relaxed = true)

    private fun chatWith(
        socket: WebSocket?,
        lastState: ChatStateEvent?,
    ): ChatWithParameters {
        val stateFlow = MutableSharedFlow<ChatStateEvent>(replay = 1)
        if (lastState != null) stateFlow.tryEmit(lastState)
        val socketFlow = MutableStateFlow(socket)
        return mockk<ChatWithParameters>(relaxed = true) {
            every { this@mockk.socket } returns socket
            every { this@mockk.socketFlow } returns socketFlow
            every { this@mockk.stateFlow } returns stateFlow
        }
    }

    @Test
    fun `returns immediately when socket is already set`() = runTest {
        val chat = chatWith(socket = mockSocket, lastState = ChatStateEvent.Ready)
        assertEquals(mockSocket, chat.awaitSocket())
    }

    @Test
    fun `throws when socket is null and state is UnexpectedDisconnect`() = runTest {
        val chat = chatWith(socket = null, lastState = ChatStateEvent.UnexpectedDisconnect)
        assertFailsWith<IllegalStateException> { chat.awaitSocket() }
    }

    @Test
    fun `throws when socket is null and no state has been emitted`() = runTest {
        val chat = chatWith(socket = null, lastState = null)
        assertFailsWith<IllegalStateException> { chat.awaitSocket() }
    }

    @Test
    fun `suspends and returns socket when state is Ready and socket arrives`() = runTest {
        val socketFlow = MutableStateFlow<WebSocket?>(null)
        val stateFlow = MutableSharedFlow<ChatStateEvent>(replay = 1).also {
            it.tryEmit(ChatStateEvent.Ready)
        }
        val chat = mockk<ChatWithParameters>(relaxed = true) {
            every { socket } returns null
            every { this@mockk.socketFlow } returns socketFlow
            every { this@mockk.stateFlow } returns stateFlow
        }

        var result: WebSocket? = null
        val job = launch { result = chat.awaitSocket() }
        socketFlow.value = mockSocket
        job.join()

        assertEquals(mockSocket, result)
    }

    @Test
    fun `throws when suspended waiting and UnexpectedDisconnect is emitted while socket stays null`() = runTest {
        // Simulates reconnection exhaustion: chat was Connecting (socket null),
        // auto-retry gave up, and the stateFlow emits UnexpectedDisconnect.
        val socketFlow = MutableStateFlow<WebSocket?>(null)
        val stateFlow = MutableSharedFlow<ChatStateEvent>(replay = 1).also {
            it.tryEmit(ChatStateEvent.Connecting)
        }
        val chat = mockk<ChatWithParameters>(relaxed = true) {
            every { socket } returns null
            every { this@mockk.socketFlow } returns socketFlow
            every { this@mockk.stateFlow } returns stateFlow
        }

        var caughtException: IllegalStateException? = null
        val job = launch {
            try {
                chat.awaitSocket()
            } catch (e: IllegalStateException) {
                caughtException = e
            }
        }
        // Advance coroutines so awaitSocket() passes the initial check and subscribes to the flows.
        runCurrent()
        // Reconnect attempts exhausted — no socket will arrive.
        stateFlow.emit(ChatStateEvent.UnexpectedDisconnect)
        job.join()

        assertEquals(true, caughtException != null, "Expected IllegalStateException on reconnection exhaustion")
    }
}
