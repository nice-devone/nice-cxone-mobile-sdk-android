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
import com.nice.cxonechat.exceptions.RuntimeChatException
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [ChatImpl.stateFlow] emission and replay behavior. Auth-body construction lives in
 * [ChatImplTest]; connection flow in [ChatImplConnectTest]; close/signOut lifecycle in
 * [ChatImplLifecycleTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImplStateFlowTest : AbstractChatImplTest() {

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits events even when rawChatStateListener is null`() = runTest {
        val chatNoListener = spyk(ChatImpl(connection, entrails, dependencies, configuration, null))
        chatNoListener.chatStateListener.onConnecting()
        assertEquals(ChatStateEvent.Connecting, chatNoListener.stateFlow.first())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow replays last event to new collector`() = runTest {
        chatImpl.chatStateListener.onReady()
        // New collector subscribes AFTER emission — should get replay
        val received = chatImpl.stateFlow.first()
        assertEquals(ChatStateEvent.Ready, received)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits Connected when onConnected is called`() = runTest {
        chatImpl.chatStateListener.onConnected()
        assertEquals(ChatStateEvent.Connected, chatImpl.stateFlow.first())
        verify { chatStateListener.onConnected() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits UnexpectedDisconnect when onUnexpectedDisconnect is called`() = runTest {
        chatImpl.chatStateListener.onUnexpectedDisconnect()
        assertEquals(ChatStateEvent.UnexpectedDisconnect, chatImpl.stateFlow.first())
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits RuntimeException wrapping the original exception`() = runTest {
        val cause = mockk<RuntimeChatException>()
        chatImpl.chatStateListener.onChatRuntimeException(cause)
        val event = chatImpl.stateFlow.first()
        assertTrue(event is ChatStateEvent.RuntimeException)
        assertSame(cause, event.exception)
        verify { chatStateListener.onChatRuntimeException(cause) }
    }
}
