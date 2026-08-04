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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.nice.cxonechat.ChatInstanceProvider.DeviceTokenProvider
import com.nice.cxonechat.ChatInstanceProvider.Listener
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChannelAvailabilityFailedException
import com.nice.cxonechat.internal.PermanentConnectionFailureException
import com.nice.cxonechat.internal.ReconnectBackoff
import com.nice.cxonechat.internal.TransientConnectFailureException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import io.mockk.Ordering.ORDERED
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass", "StringLiteralDuplication")
internal class ChatInstanceProviderTest {
    private val applicationContext = mockk<Context> {
        every { applicationContext } returns this@mockk
    }
    private lateinit var testDispatcher: TestDispatcher
    private val baseUrl = "https://chat.server/"
    private val chatUrl by lazy { "${baseUrl}chat/" }
    private val loggerUrl by lazy { "${baseUrl}logger-public" }
    private val socketEnvironment: Environment by lazy {
        mockk<Environment> {
            every { chatUrl } returns this@ChatInstanceProviderTest.chatUrl
            every { loggerUrl } returns this@ChatInstanceProviderTest.loggerUrl
        }
    }
    private val socketFactoryConfiguration by lazy {
        SocketFactoryConfiguration(
            environment = socketEnvironment,
            brandId = BRAND_ID,
            channelId = CHANNEL_ID
        )
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>(relaxed = true)
        val mockBuilder = mockk<Uri.Builder>(relaxed = true)
        every { Uri.parse(any()) } returns mockUri
        every { mockUri.buildUpon() } returns mockBuilder
        every { mockBuilder.appendQueryParameter(any(), any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockUri
        every { mockUri.toString() } returns "https://mocked-uri"
    }

    /** Advances the test dispatcher to complete all pending coroutines launched by ChatInstanceProvider. */
    private fun advanceUntilIdle() = testDispatcher.scheduler.advanceUntilIdle()

    @Suppress("LABEL_NAME_CLASH", "DEPRECATION")
    private fun provider(
        socketFactoryConfiguration: SocketFactoryConfiguration? = this.socketFactoryConfiguration,
        logger: Logger = mockk(relaxed = true),
        isOnline: Boolean = true,
        dispatcher: TestDispatcher = testDispatcher,
        onConnected: (ChatStateListener?) -> Unit = { },
    ): Pair<ChatInstanceProvider, ChatBuilder> {
        val builder = mockk<ChatBuilder> {
            val builder = this
            var listener: ChatStateListener? = null

            every { setUserName(any(), any()) } returns this
            every { setAuthorization(any()) } returns this
            every { setDevelopmentMode(any()) } returns this
            every { setChatStateListener(any()) } answers {
                listener = arg<ChatStateListener?>(0)
                builder
            }
            every { setDeviceToken(any()) } returns this
            coEvery { build() } answers {
                mockk<Chat>(relaxUnitFun = true) {
                    coEvery { connect() } answers {
                        listener?.onConnecting()
                        onConnected(listener)
                    }
                    every { configuration } answers {
                        mockk {
                            every { isAuthorizationEnabled } returns false
                            every { this@mockk.isOnline } returns isOnline
                        }
                    }
                    every { isChatAvailable } returns isOnline
                    every { environment } returns socketEnvironment
                }
            }
        }
        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = dispatcher,
        )
        return Pair(provider, builder)
    }

    @Test
    fun createRegistersInstance() {
        assertSame(
            ChatInstanceProvider.create(null),
            ChatInstanceProvider.get(),
            "ChatInstanceProvider.create() should return same object as ChatInstanceProvider.get()"
        )
    }

    @Test
    fun initialStateIsInitial() {
        assertEquals(
            ChatInstanceProvider.create(null).chatState,
            Initial
        )
    }

    @Test(expected = InvalidStateException::class)
    fun prepareThrowsWithNoConfiguration() {
        provider(null).first.prepare(mockk())
    }

    @Test
    fun configurationAuthenticationRequiredDefaultsFalse() = runTest(testDispatcher) {
        val (provider) = provider(socketFactoryConfiguration)

        provider.configure(applicationContext) {
            assertFalse(authenticationRequired)
        }
    }

    @Test
    fun configureAccessAuthenticationRequired() = runTest(testDispatcher) {
        listOf(true, false).forEach { required ->
            val (provider) = provider(socketFactoryConfiguration)

            provider.prepare(applicationContext)
            advanceUntilIdle()

            val chat = requireNotNull(provider.chat)

            every { chat.configuration.isAuthorizationEnabled } returns required

            provider.configure(applicationContext) {
                assertEquals(required, authenticationRequired)
            }
        }
    }

    @Test
    fun configureSetsConfiguration() = runTest(testDispatcher) {
        val (provider, _) = provider(null)

        provider.configure(applicationContext) {
            configuration = socketFactoryConfiguration
            assertSame(socketFactoryConfiguration, configuration)
        }

        assertSame(provider.configuration, socketFactoryConfiguration)
    }

    @Test
    fun configureSetsUserName() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = UserName("first name", "last name")

        provider.configure(applicationContext) {
            userName = expect
            assertSame(expect, userName)
        }

        assertEquals(expect, provider.userName)
    }

    @Test
    fun configureSetsLogger() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = mockk<Logger>()

        provider.configure(applicationContext) {
            logger = expect
            assertSame(expect, logger)
        }

        assertEquals(expect, provider.logger)
    }

    @Test
    fun configureSetsAuthorization() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = Authorization("code", "verifier")

        provider.configure(applicationContext) {
            authorization = expect
            assertSame(expect, authorization)
        }

        assertEquals(expect, provider.authorization)
    }

    @Test
    fun configureSetsDevelopmentMode() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = !provider.developmentMode

        provider.configure(applicationContext) {
            developmentMode = expect
            assertEquals(expect, developmentMode)
        }

        assertEquals(expect, provider.developmentMode)
    }

    @Test
    fun configureSetsTokenProvider() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = mockk<DeviceTokenProvider> {
            every { requestDeviceToken(any()) } just runs
        }

        provider.configure(applicationContext) {
            deviceTokenProvider = expect
            assertSame(expect, deviceTokenProvider)
        }

        assertSame(expect, provider.deviceTokenProvider)
    }

    @Test
    fun configureRestartsChat() = runTest(testDispatcher) {
        val (provider, _) = provider(socketFactoryConfiguration)

        assertNull(provider.chat)

        provider.configure(applicationContext) {}
        advanceUntilIdle()

        val chat = requireNotNull(provider.chat)

        provider.configure(applicationContext) {}
        advanceUntilIdle()

        coVerify {
            chat.signOut()
        }

        assertNotSame(chat, provider.chat)
        assertEquals(Prepared, provider.chatState)
    }

    @SuppressLint("CheckResult")
    @Test
    fun configureSetsUpBuilder() = runTest(testDispatcher) {
        val token = "token"
        val tokenProvider = DeviceTokenProvider { it(token) }
        val (provider, builder) = provider(socketFactoryConfiguration)
        val auth = Authorization("code", "verifier")

        provider.configure(applicationContext) {
            developmentMode = false
            userName = UserName(lastName = "last", firstName = "first")
            authorization = auth
            deviceTokenProvider = tokenProvider
        }
        advanceUntilIdle()

        coVerifyOrder {
            // Note that the order of the sets is not required, it is just required that
            // they all occur before the build call.
            builder.setChatStateListener(provider)
            builder.setDevelopmentMode(false)
            builder.setUserName("first", "last")
            builder.setAuthorization(auth)
            builder.setDeviceToken(token)
            builder.build()
            provider.chat?.setDeviceToken(token)
        }
    }

    @Test
    @SuppressLint("CheckResult")
    fun prepareAdvancesChatState() {
        val (provider, builder) = provider(socketFactoryConfiguration)

        val listener = mockk<Listener> {
            every { onChatChanged(any()) } just runs
            every { onChatStateChanged(any()) } just runs
            every { onChatStateChanged(any()) } just runs
        }.also(provider::addListener)

        provider.prepare(applicationContext)
        advanceUntilIdle()

        coVerify(ordering = ORDERED) {
            builder.build()
            listener.onChatChanged(any())
            listener.onChatStateChanged(Prepared)
        }
    }

    @Test
    fun connectionLostCancels() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        provider.onUnexpectedDisconnect()
        provider.cancel()

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun initialIgnoresCancel() {
        val (provider) = provider(null)

        assertEquals(Initial, provider.chatState)

        provider.cancel()

        assertEquals(Initial, provider.chatState)
    }

    @Test
    fun preparedIgnoresCancel() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()

        assertEquals(Prepared, provider.chatState)

        provider.cancel()

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun connectedIgnoresCancel() {
        val (provider) = provider(socketFactoryConfiguration, onConnected = { })

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        provider.onConnected()

        assertEquals(Connected, provider.chatState)

        provider.cancel()

        assertEquals(Connected, provider.chatState)
    }

    @SuppressLint("CheckResult")
    @Test
    fun duplicatePrepareIgnored() {
        val (provider, builder) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()

        provider.prepare(applicationContext)

        coVerify(exactly = 1) {
            builder.build()
        }
    }

    @Test
    fun setUserNameForwardsToChat() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()

        provider.setUserName(UserName(lastName = "last", firstName = "first"))

        verify {
            provider.chat!!.setUserName("first", "last")
        }
    }

    @Test(expected = InvalidStateException::class)
    fun connectThrowsInInitial() {
        val (provider) = provider(null)

        assertEquals(Initial, provider.chatState)

        provider.connect()
    }

    @Test(expected = InvalidStateException::class)
    fun connectThrowsInPreparing() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)

        assertEquals(Preparing, provider.chatState)

        provider.connect()
    }

    @Test(expected = InvalidStateException::class)
    fun connectThrowsWhenConnecting() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()
        provider.connect()
    }

    @Suppress("DEPRECATION")
    @Test
    fun connectIgnoredWhenConnected() {
        val logger: Logger = mockk(relaxed = true)
        var connectCallCount = 0
        val builder = mockk<ChatBuilder>()
        var chatStateListener: ChatStateListener? = null
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } answers {
            chatStateListener = arg<ChatStateListener>(0)
            builder
        }
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } answers {
            connectCallCount++
            chatStateListener?.onConnecting()
        }

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()
        provider.onConnected()
        assertEquals(1, connectCallCount)
    }

    @Test
    fun onConnectedAdvancesState() {
        val (provider, _) = provider(socketFactoryConfiguration, onConnected = { listener ->
            listener?.onConnected()
        })
        val listener = mockk<Listener>(relaxUnitFun = true)

        provider.addListener(listener)
        provider.addListener(object : Listener {})
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle() // This will trigger onConnecting and onConnected via the mock

        verify {
            listener.onChatStateChanged(Connected)
        }
    }

    @Test
    fun onReadyAdvancesState() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)

        provider.addListener(listener)
        // this does nothing, but it makes coverage happy
        provider.addListener(object : Listener {})
        provider.prepare(applicationContext)
        advanceUntilIdle()

        provider.onReady()

        verify {
            listener.onChatStateChanged(Ready)
        }
    }

    @Test
    fun onDisconnectAdvancesState() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)

        provider.addListener(listener)
        // this does nothing, but it makes coverage happy
        provider.addListener(object : Listener {})
        provider.prepare(applicationContext)
        advanceUntilIdle()

        provider.onUnexpectedDisconnect()

        verify {
            listener.onChatStateChanged(ConnectionLost)
        }
    }

    @Test
    fun onExceptionForwards() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)
        val exception = mockk<RuntimeChatException>()

        provider.addListener(listener)
        // this does nothing, but it makes coverage happy
        provider.addListener(object : Listener {})
        provider.prepare(applicationContext)
        advanceUntilIdle()

        provider.onChatRuntimeException(exception)

        verify {
            listener.onChatRuntimeException(exception)
        }
    }

    @Test
    fun closeClosesChat() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        val chat = requireNotNull(provider.chat)
        provider.close()

        coVerify {
            chat.closeSuspending()
        }

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun removeListener() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxed = true)

        provider.addListener(listener)
        provider.removeListener(listener)
        provider.onConnected()

        verify(exactly = 0) {
            listener.onChatStateChanged(any())
        }
    }

    @Test
    fun prepareAdvancesStateAndCancels() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)

        assertEquals(Preparing, provider.chatState)

        provider.cancel()

        assertEquals(Initial, provider.chatState)
    }

    @Suppress("DEPRECATION")
    @Test
    fun connectAdvancesStateAndCancels() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } answers {
            builder
        }
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } answers { }

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        // Wait for the coroutine in doConnect() to complete
        advanceUntilIdle()
        assertEquals(Connecting, provider.chatState)
        provider.cancel()
        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun setCustomerValuesSurvivesNoChat() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.setCustomerValues(mapOf())
    }

    @Test
    fun advanceState_cancels() {
        val provider = ChatInstanceProvider.create(null)
        val cancellable = mockk<Cancellable> {
            every { cancel() } just Runs
        }

        provider.advanceState(Connecting, cancellable, false)

        provider.advanceState(Connected)

        verify {
            cancellable.cancel()
        }
    }

    @Test
    fun advanceState_requiredCancellables() {
        for (state in ChatState.entries) {
            val provider = ChatInstanceProvider.create(null)
            var thrown = false

            if (state == Initial) {
                provider.advanceState(ConnectionLost)
            }

            @Suppress("SwallowedException")
            try {
                provider.advanceState(state)
            } catch (_: AssertionError) {
                thrown = true
            }

            if (setOf(Connecting, Preparing).contains(state)) {
                assertTrue(thrown, "Missing expected exception for null cancellable in $state")
            } else {
                assertFalse(thrown, "Unexpected exception for null cancellable in $state")
            }
        }
    }

    @Test
    fun advanceState_disallowedCancellables() {
        for (state in ChatState.entries) {
            val provider = ChatInstanceProvider.create(null)
            var thrown = false

            if (state == Initial) {
                provider.advanceState(ConnectionLost)
            }

            @Suppress("SwallowedException")
            try {
                provider.advanceState(state, mockk())
            } catch (_: AssertionError) {
                thrown = true
            }

            if (setOf(Connecting, Preparing).contains(state)) {
                assertFalse(thrown, "Unexpected exception for disallowed cancellable in $state")
            } else {
                assertTrue(thrown, "Missing expected exception for disallowed cancellable in $state")
            }
        }
    }

    @Test
    fun advanceState_notifiesListeners() {
        val provider = ChatInstanceProvider.create(null)
        val listener = mockk<Listener> {
            every { onChatStateChanged(any()) } just Runs
        }.also(provider::addListener)

        provider.advanceState(Connected)

        verify {
            listener.onChatStateChanged(Connected)
        }
    }

    @Test
    fun connectHandlesOffline() {
        val (provider) = provider(socketFactoryConfiguration, isOnline = false) {
            it?.onReady()
        }

        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        assertEquals(Offline, provider.chatState)

        provider.cancel()

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun onChatChangedNotifiesListener() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)
        provider.addListener(listener)
        provider.prepare(applicationContext)
        advanceUntilIdle()
        verify { listener.onChatChanged(provider.chat) }
    }

    @Test
    fun deviceTokenProviderAsyncCallback() = runTest(testDispatcher) {
        var callback: ((String) -> Unit)? = null
        val tokenProvider = DeviceTokenProvider { cb -> callback = cb }
        val (provider, builder) = provider(socketFactoryConfiguration)
        provider.configure(applicationContext) {
            deviceTokenProvider = tokenProvider
        }
        advanceUntilIdle()
        callback?.invoke("async-token")
        verify { builder.setDeviceToken("async-token") }
        verify { provider.chat?.setDeviceToken("async-token") }
    }

    @Test
    fun removeMultipleListeners() {
        val (provider) = provider(socketFactoryConfiguration, onConnected = { })
        val listener1 = mockk<Listener>(relaxUnitFun = true)
        val listener2 = mockk<Listener>(relaxUnitFun = true)
        provider.addListener(listener1)
        provider.addListener(listener2)
        provider.removeListener(listener1)
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        provider.onConnected()
        verify(exactly = 0) { listener1.onChatStateChanged(any()) }
        verify { listener2.onChatStateChanged(Connected) }
    }

    @Test
    fun signOutNotifiesListeners() = runTest(testDispatcher) {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)
        provider.addListener(listener)
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.signOut()
        verify { listener.onChatChanged(null) }
        verify { listener.onChatStateChanged(Initial) }
    }

    @Test
    fun createReplacesSingleton() {
        val instance1 = ChatInstanceProvider.create(socketFactoryConfiguration)
        val instance2 = ChatInstanceProvider.create(socketFactoryConfiguration)
        assertNotSame(instance1, instance2)
        assertSame(instance2, ChatInstanceProvider.get())
    }

    @Test(expected = IllegalStateException::class)
    fun getThrowsIfNotCreated() {
        // Reset singleton for test
        val field = ChatInstanceProvider::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
        ChatInstanceProvider.get()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `connect failure transitions to ConnectionLost`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } throws TransientConnectFailureException("Socket failed to open", RuntimeException("refused"))

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        assertEquals(ConnectionLost, provider.chatState)
    }

    // --- Pre-socket REST connect failure auto-retry ---
    // ChannelAvailabilityFailedException must not leave the provider stuck in ConnectionLost —
    // PreSocketReconnectScheduler retries with backoff, no second UI-driven connect() needed.

    @Suppress("DEPRECATION")
    @Test
    fun `pre-socket availability failure automatically retries and reaches Ready without a second connect call`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)
        var listener: ChatStateListener? = null
        var connectCallCount = 0

        every { builder.setChatStateListener(any()) } answers {
            listener = arg<ChatStateListener?>(0)
            builder
        }
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } answers {
            connectCallCount++
            if (connectCallCount == 1) {
                throw ChannelAvailabilityFailedException("Offline", IOException("Unable to resolve host"))
            } else {
                listener?.onConnected()
                listener?.onReady()
            }
        }

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()

        // Single UI-driven connect() call — the second attempt must come from the scheduler itself.
        provider.connect()
        advanceUntilIdle()

        assertEquals(Ready, provider.chatState)
        coVerify(exactly = 2) { chat.connect() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `pre-socket auto-retry delay follows the shared backoff formula, not a fast-retry loop`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } throws ChannelAvailabilityFailedException("Offline", IOException("Unable to resolve host"))

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        val fixedJitter = 3000L
        provider.preSocketReconnectScheduler.randomDelayProvider = { fixedJitter }
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        // runCurrent() (not advanceUntilIdle()) — chat.connect() always throws, so each retry
        // reschedules another one; advanceUntilIdle() would drain the whole 20-attempt chain and
        // land on the FINAL backoff value instead of the first. runCurrent() executes only what's
        // due at the current virtual instant: the failure and the scheduling of the first retry,
        // stopping at its delay() suspension point.
        testDispatcher.scheduler.runCurrent()

        assertEquals(ConnectionLost, provider.chatState)
        // Must match ReconnectBackoff's formula (same as the socket-level ReconnectingListener uses) —
        // never a fixed short delay, which would violate the backend's retry-rate-limit requirement.
        assertEquals(
            ReconnectBackoff.INITIAL_DELAY + fixedJitter,
            provider.preSocketReconnectScheduler.currentDelayMillis,
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun `cancel prevents a scheduled pre-socket auto-retry from firing`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)
        var connectCallCount = 0

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } answers {
            connectCallCount++
            throw ChannelAvailabilityFailedException("Offline", IOException("Unable to resolve host"))
        }

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        // runCurrent(), not advanceUntilIdle() — see the sibling backoff-formula test for why.
        testDispatcher.scheduler.runCurrent()

        assertEquals(ConnectionLost, provider.chatState)
        assertEquals(1, connectCallCount)

        // Cancel before the scheduled retry's backoff delay elapses — it must not fire afterward.
        provider.cancel()
        advanceUntilIdle()

        assertEquals(Prepared, provider.chatState)
        assertEquals(1, connectCallCount)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stale pre-socket failure arriving after cancel does not schedule a retry`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)
        var connectCallCount = 0
        lateinit var provider: ChatInstanceProvider

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } answers {
            connectCallCount++
            // The real getAvailability() call is a blocking, non-cancellable execute() — it can
            // still throw after the caller already cancelled. Simulate that ordering directly:
            // cancel() runs (state Connecting -> Prepared) while this "in-flight" call is the one
            // about to fail.
            provider.cancel()
            throw ChannelAvailabilityFailedException("Offline", IOException("Unable to resolve host"))
        }

        provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        assertEquals(Prepared, provider.chatState)
        assertEquals(1, connectCallCount)

        // No retry should have been scheduled for a failure that arrived after cancel() — advancing
        // well past the backoff window must not produce a second connect() call.
        advanceUntilIdle()
        assertEquals(1, connectCallCount)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `permanent auth failure transitions to ConnectionLost`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } throws PermanentConnectionFailureException("Auth failed", RuntimeException("ConsumerReconnectionFailed"))

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        assertEquals(ConnectionLost, provider.chatState)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `mid-auth CancellationException transitions provider to ConnectionLost`() {
        val logger = mockk<Logger>(relaxed = true)
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>(relaxUnitFun = true)

        every { builder.setChatStateListener(any()) } returns builder
        every { builder.setUserName(any(), any()) } returns builder
        every { builder.setAuthorization(any()) } returns builder
        every { builder.setDevelopmentMode(any()) } returns builder
        every { builder.setDeviceToken(any()) } returns builder
        coEvery { builder.build() } returns chat
        every { chat.configuration } returns mockk {
            every { isAuthorizationEnabled } returns false
            every { this@mockk.isOnline } returns true
        }
        every { chat.isChatAvailable } returns true
        every { chat.environment } returns socketEnvironment
        coEvery { chat.connect() } throws CancellationException("Socket dropped during auth")

        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            logger = logger,
            chatBuilderProvider = { _, _, _ -> builder },
            dispatcher = testDispatcher,
        )
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        assertEquals(ConnectionLost, provider.chatState)
    }

    @Test
    fun `onConnected called from within connect coroutine does not cancel it`() {
        val provider =
            provider(onConnected = { listener ->
                listener?.onConnected()
                listener?.onReady() // Only runs if the connect coroutine was not cancelled by advanceState
            }
            ).first
        val chatListener = mockk<Listener>(relaxUnitFun = true)
        provider.addListener(chatListener)
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        verify { chatListener.onChatStateChanged(Ready) }
    }

    // --- Regression tests for DE-168021: doConnect() race condition ---
    // Without CoroutineStart.LAZY, the launched coroutine runs before advanceState(Connecting),
    // so a fast failure completes onConnectLost() while chatState is still Prepared → guard
    // misses → ConnectionLost is never set. Then advanceState(Connecting) runs afterward,
    // leaving the provider permanently stuck in Connecting.
    // Fix: launch(start = LAZY) + job.start() after advanceState(Connecting) ensures state
    // is Connecting before the coroutine body can run.
    //
    // UnconfinedTestDispatcher is required here: it runs coroutines eagerly (synchronously on
    // the calling thread), reproducing the race. StandardTestDispatcher queues them, so
    // advanceState(Connecting) always runs first and the race is invisible.

    @Test
    fun `connect failure with synchronous dispatch does not leave provider in Connecting state`() {
        val (provider) = provider(
            onConnected = { throw TransientConnectFailureException("Socket refused", RuntimeException("refused")) },
            dispatcher = UnconfinedTestDispatcher(),
        )
        provider.prepare(applicationContext)
        provider.connect()

        assertNotEquals(Connecting, provider.chatState)
    }

    @Test
    fun `connect failure with synchronous dispatch transitions provider to ConnectionLost`() {
        val (provider) = provider(
            onConnected = { throw TransientConnectFailureException("Socket refused", RuntimeException("refused")) },
            dispatcher = UnconfinedTestDispatcher(),
        )
        provider.prepare(applicationContext)
        provider.connect()

        assertEquals(ConnectionLost, provider.chatState)
    }

    @Test
    fun `connect after close rebuilds the chat instance`() {
        val (provider, builder) = provider()
        provider.prepare(applicationContext)
        advanceUntilIdle()
        val sessionOneChat = provider.chat
        provider.connect()
        advanceUntilIdle()

        provider.close() // app goes to background: session ends
        provider.connect() // return to foreground
        advanceUntilIdle()

        assertNotNull(provider.chat)
        assertNotSame(sessionOneChat, provider.chat, "connect() after close() must rebuild the Chat instance")
        coVerify(exactly = 2) { builder.build() }
    }

    @Test
    fun `connect after close notifies listeners about the new chat instance`() {
        val (provider, _) = provider()
        val observedChats = mutableListOf<Chat?>()
        val listener = object : Listener {
            override fun onChatChanged(chat: Chat?) {
                observedChats += chat
            }
        }
        provider.addListener(listener)
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        provider.close()
        provider.connect()
        advanceUntilIdle()

        assertEquals(2, observedChats.size, "onChatChanged must fire for the initial build and the rebuild")
        assertNotSame(observedChats[0], observedChats[1])
        assertSame(provider.chat, observedChats[1])
    }

    @Test
    fun `reconnect from connection lost does not rebuild the chat instance`() {
        val (provider, builder) = provider()
        provider.prepare(applicationContext)
        advanceUntilIdle()
        val sessionChat = provider.chat
        provider.connect()
        advanceUntilIdle()

        provider.onUnexpectedDisconnect() // socket dropped — close() was never called
        provider.connect()
        advanceUntilIdle()

        assertSame(sessionChat, provider.chat, "ConnectionLost reconnect must reuse the live Chat instance")
        coVerify(exactly = 1) { builder.build() }
    }

    @Test
    fun `cancel during rebuild keeps the rebuild pending for the next connect`() {
        val (provider, builder) = provider()
        provider.prepare(applicationContext)
        advanceUntilIdle()
        val sessionOneChat = provider.chat
        provider.connect()
        advanceUntilIdle()

        provider.close()
        provider.connect() // rebuild scheduled but not yet executed (StandardTestDispatcher)
        provider.cancel() // user backs out mid-connect
        advanceUntilIdle()
        assertEquals(Prepared, provider.chatState)

        provider.connect() // next attempt must still rebuild
        advanceUntilIdle()
        assertNotSame(sessionOneChat, provider.chat)
        coVerify(exactly = 2) { builder.build() }
    }

    @Test
    fun `sign out clears the pending rebuild`() = runTest(testDispatcher) {
        val (provider, builder) = provider()
        provider.prepare(applicationContext)
        advanceUntilIdle()
        provider.connect()
        advanceUntilIdle()

        provider.close() // flags a rebuild
        provider.signOut() // discards the session entirely
        provider.prepare(applicationContext) // fresh prepare builds anew
        advanceUntilIdle()
        val preparedChat = provider.chat
        provider.connect() // must NOT rebuild again — prepare's instance is fresh
        advanceUntilIdle()

        assertSame(preparedChat, provider.chat)
        coVerify(exactly = 2) { builder.build() }
    }

    @Test
    fun `prepare forwards TokenDelegateListener to Builder`() = runTest {
        val (provider, builder) = provider()
        every { builder.setTokenDelegateListener(any()) } returns builder

        val listener = TokenDelegateListener { OAuthToken("token") }
        provider.setTokenDelegateListener(listener)
        provider.prepare(applicationContext)
        advanceUntilIdle()

        verify { builder.setTokenDelegateListener(listener) }
    }

    @Test
    fun `configure TokenDelegateListener persists across restart`() = runTest {
        val (provider, builder) = provider()
        every { builder.setTokenDelegateListener(any()) } returns builder

        val listener = TokenDelegateListener { OAuthToken("token") }
        provider.configure(applicationContext) {
            tokenDelegateListener = listener
        }
        advanceUntilIdle()

        verify { builder.setTokenDelegateListener(listener) }
        assertEquals(listener, provider.tokenDelegateListener)
    }

    @Test
    fun `signOut clears customerId to prevent prior user identity leaking into the next session`() = runTest {
        val provider = ChatInstanceProvider.create(
            configuration = socketFactoryConfiguration,
            customerId = "user-123",
        )

        assertEquals("user-123", provider.customerId, "customerId should be set before signOut")
        provider.signOut()

        assertNull(provider.customerId, "customerId must be null after signOut")
    }

    companion object {
        private const val BRAND_ID = 1000L
        private val CHANNEL_ID = UUID.randomUUID().toString()
    }
}
