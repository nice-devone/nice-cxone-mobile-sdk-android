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

import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatActionHandlerTest : AbstractMultiThreadChatTest() {

    private lateinit var actions: ChatActionHandler

    override fun prepare() {
        super.prepare()
        actions = chat.actions()
    }

    @After
    fun tearDown() {
        actions.close()
    }

    @Test
    fun popupFlow_emits_newPopups() = runTest {
        val expected = mapOf("param" to "my param value")
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(ServerResponse.ActionPopup(expected))
        advanceUntilIdle()
        assertEquals(expected, result.await().variables)
    }

    @Test
    fun popupFlow_replays_latestUnhandledPopup() = runTest {
        val expected = mapOf("param" to "my param value")
        serverResponds(ServerResponse.ActionPopup(expected))
        advanceUntilIdle()
        val event = actions.popupFlow.first()
        assertEquals(expected, event.variables)
    }

    @Test
    fun popupFlow_delivers_afterReconnect() = runTest {
        // The global action handler registers a persistent socket-listener callback (addCallback ->
        // addListener on the long-lived ProxyWebSocketListener), not a chat-scope coroutine, so the
        // same cached handler must keep delivering proactive popups across a reconnect on the same
        // Chat instance — it is not affected by the thread-variant bug where a chat-scope listener
        // job died on Chat.close().
        val expected = mapOf("param" to "value after reconnect")
        reconnect()
        advanceUntilIdle()
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(ServerResponse.ActionPopup(expected))
        advanceUntilIdle()
        assertEquals(expected, result.await().variables)
    }

    @Test
    fun popupFlow_returns_metadata() = runTest {
        val id = UUID.randomUUID()
        val name = nextString()
        val type = ActionType.CustomPopupBox
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        val popup = ServerResponse.ActionPopup(
            params = emptyMap(),
            actionId = id,
            actionName = name,
            actionType = type
        )
        serverResponds(popup)
        advanceUntilIdle()
        val expected = ActionMetadataInternal(id, name, type)
        assertEquals(expected, result.await().metadata)
    }
}
