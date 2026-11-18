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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.tool.MockLogger
import io.mockk.clearMocks
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Before

/**
 * Shared fixtures for the [ReconnectingListener] test classes: the test dispatcher, mocked
 * [ChatStateListener], the [connectCalled] counter, the [Before]/[After] setup, and the private-field
 * reflection helpers plus [TestException] used across the delay and state-change scenarios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class AbstractReconnectListenerTest {
    protected lateinit var dispatcher: TestDispatcher
    protected lateinit var chatStateListener: ChatStateListener
    protected var connectCalled: Int = 0
    protected lateinit var reconnectListener: ReconnectingListener

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        chatStateListener = mockk(relaxed = true)
        connectCalled = 0
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
            }
        )
    }

    @After
    fun tearDown() {
        clearMocks(chatStateListener)
    }

    protected fun ReconnectingListener.getWasEverConnected() = ReconnectingListener::class.java
        .getDeclaredField("wasEverConnected")
        .apply { isAccessible = true }
        .let { field ->
            (field.get(this) as java.util.concurrent.atomic.AtomicBoolean).get()
        }

    protected fun ReconnectingListener.getCurrentDelayMillis() = ReconnectingListener::class.java
        .getDeclaredField("currentDelayMillis")
        .apply { isAccessible = true }
        .getLong(this)

    protected fun ReconnectingListener.setCurrentDelayMillis(value: Long) {
        ReconnectingListener::class.java
            .getDeclaredField("currentDelayMillis")
            .apply { isAccessible = true }
            .setLong(this, value)
    }

    protected class TestException(message: String) : Exception(message)
}
