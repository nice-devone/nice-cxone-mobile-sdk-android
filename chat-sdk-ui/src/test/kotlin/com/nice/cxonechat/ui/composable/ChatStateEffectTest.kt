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

package com.nice.cxonechat.ui.composable

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Ready
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for DE-178452: [ChatStateEffect] must not keep invoking its connect action while a
 * terminal (HIGH/HIGH_SPECIFIC) error dialog is blocking the screen, even though the lifecycle stays
 * resumed and the chat state keeps re-entering the states that would otherwise trigger auto-reconnect —
 * and it must resume auto-reconnect once that dialog clears, even if [ChatState] and lifecycle never change
 * in the meantime, without spuriously re-firing the one-shot [Ready]/[Offline] actions in either direction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChatStateEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `does not auto-reconnect from ConnectionLost while a terminal error dialog is showing`() {
        assertNoConnectAttempt(state = ConnectionLost)
    }

    @Test
    fun `does not auto-reconnect from Prepared while a terminal error dialog is showing`() {
        assertNoConnectAttempt(state = Prepared)
    }

    @Test
    fun `does not auto-reconnect from Connecting while a terminal error dialog is showing`() {
        assertNoConnectAttempt(state = Connecting)
    }

    @Test
    fun `does not auto-reconnect from Offline while a terminal error dialog is showing, but still reports offline`() {
        var connectCount = 0
        var offlineCount = 0
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(Offline),
                isAutoReconnectBlocked = true,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = { offlineCount++ },
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(0, connectCount)
        assertEquals(1, offlineCount)
    }

    @Test
    fun `auto-reconnects from ConnectionLost when no terminal error is showing`() {
        var connectCount = 0
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(ConnectionLost),
                isAutoReconnectBlocked = false,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(1, connectCount)
    }

    @Test
    fun `Ready action is unaffected by the terminal error flag`() {
        var readyCount = 0
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(Ready),
                isAutoReconnectBlocked = true,
                onConnectChatAction = {},
                onReadyAction = { readyCount++ },
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(1, readyCount)
    }

    @Test
    fun `Ready action does not re-fire when the terminal error flag toggles with state unchanged`() {
        var readyCount = 0
        val isAutoReconnectBlocked = mutableStateOf(false)
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(Ready),
                isAutoReconnectBlocked = isAutoReconnectBlocked.value,
                onConnectChatAction = {},
                onReadyAction = { readyCount++ },
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(1, readyCount)

        isAutoReconnectBlocked.value = true
        composeTestRule.waitForIdle()
        isAutoReconnectBlocked.value = false
        composeTestRule.waitForIdle()

        assertEquals(1, readyCount)
    }

    @Test
    fun `does not auto-reconnect across a ConnectionLost-Connecting-ConnectionLost loop while a terminal error dialog is showing`() {
        var connectCount = 0
        val chatStateFlow = MutableStateFlow(ConnectionLost)
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = chatStateFlow,
                isAutoReconnectBlocked = true,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()

        chatStateFlow.value = Connecting
        composeTestRule.waitForIdle()
        chatStateFlow.value = ConnectionLost
        composeTestRule.waitForIdle()

        assertEquals(0, connectCount)
    }

    @Test
    fun `resumes auto-reconnect once the terminal error dialog clears without a state change`() {
        var connectCount = 0
        val isAutoReconnectBlocked = mutableStateOf(true)
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(ConnectionLost),
                isAutoReconnectBlocked = isAutoReconnectBlocked.value,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(0, connectCount)

        isAutoReconnectBlocked.value = false
        composeTestRule.waitForIdle()

        assertEquals(1, connectCount)
    }

    @Test
    fun `resumes auto-reconnect from Offline once the terminal error dialog clears, without re-firing onOfflineAction`() {
        var connectCount = 0
        var offlineCount = 0
        val isAutoReconnectBlocked = mutableStateOf(true)
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(Offline),
                isAutoReconnectBlocked = isAutoReconnectBlocked.value,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = { offlineCount++ },
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(0, connectCount)
        assertEquals(1, offlineCount)

        isAutoReconnectBlocked.value = false
        composeTestRule.waitForIdle()

        assertEquals(1, connectCount)
        assertEquals(1, offlineCount)
    }

    @Test
    fun `suppresses auto-reconnect as soon as the terminal error dialog appears, without a state change`() {
        var connectCount = 0
        val isAutoReconnectBlocked = mutableStateOf(false)
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(ConnectionLost),
                isAutoReconnectBlocked = isAutoReconnectBlocked.value,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(1, connectCount)

        isAutoReconnectBlocked.value = true
        composeTestRule.waitForIdle()

        assertEquals(1, connectCount)
    }

    private fun assertNoConnectAttempt(state: ChatState) {
        var connectCount = 0
        composeTestRule.setContent {
            ChatStateEffect(
                chatStateFlow = MutableStateFlow(state),
                isAutoReconnectBlocked = true,
                onConnectChatAction = { connectCount++ },
                onReadyAction = {},
                onOfflineAction = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(0, connectCount)
    }
}
