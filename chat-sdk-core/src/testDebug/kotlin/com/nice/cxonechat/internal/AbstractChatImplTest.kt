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
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.ChatEntrailsMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before

/**
 * Shared fixtures for the [ChatImpl] test classes: the mocked dependency graph, the spied
 * [chatImpl] under test, the [Before] setup, and the [startDaemon] helper for the thread-based
 * lifecycle tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class AbstractChatImplTest {
    protected lateinit var chatImpl: ChatImpl
    protected lateinit var connection: Connection
    protected lateinit var entrails: ChatEntrails
    protected lateinit var dependencies: ChatImplDependencies
    protected lateinit var configuration: ConfigurationInternal
    protected lateinit var chatStateListener: ChatStateListener
    protected lateinit var storage: ValueStorage
    protected lateinit var retryApiHandler: RetryApiHandler

    protected lateinit var authService: AuthService

    @Before
    fun setUp() {
        connection = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        entrails = ChatEntrailsMock(
            sharedClient = mockk(relaxed = true),
            storage = storage,
            service = mockk(relaxed = true),
            authService = authService,
            logger = mockk(relaxed = true),
            environment = mockk(relaxed = true),
            cookieJar = mockk(relaxed = true),
            coroutineScope = TestScope(StandardTestDispatcher()),
        )
        // Use a real ProxyWebSocketListener so that SocketConnectionListener callbacks work correctly
        // (needed since connect() now suspends at openDeferred.await() until onOpen fires).
        val proxyListener = ProxyWebSocketListener()
        dependencies = mockk(relaxed = true) {
            every { socketFactory.createProxyListener() } returns proxyListener
        }
        configuration = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        chatImpl = spyk(ChatImpl(connection, entrails, dependencies, configuration, chatStateListener))
        retryApiHandler = mockk(relaxed = true)
        chatImpl.retryApiHandler = retryApiHandler
    }

    protected fun Thread.startDaemon() = apply {
        isDaemon = true
        start()
    }
}
