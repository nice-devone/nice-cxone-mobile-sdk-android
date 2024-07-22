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

package com.nice.cxonechat

import android.annotation.SuppressLint
import android.content.Context
import com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback
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
import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.state.FieldDefinition
import io.mockk.Ordering.ORDERED
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("LargeClass", "StringLiteralDuplication")
internal class ChatInstanceProviderTest {
    private val applicationContext = mockk<Context>()
    private val socketEnvironment: Environment by lazy {
        mockk()
    }
    private val socketFactoryConfiguration by lazy {
        object : SocketFactoryConfiguration {
            override val environment = socketEnvironment
            override val brandId = BRAND_ID
            override val channelId = CHANNEL_ID

            @Deprecated("This field is deprecated for public usage and will be removed from public API.")
            override val version = VERSION
        }
    }

    private fun provider(
        socketFactoryConfiguration: SocketFactoryConfiguration? = this.socketFactoryConfiguration,
        onBuilt: (Chat, OnChatBuiltResultCallback?) -> Cancellable = { chat, callback ->
            callback?.onChatBuiltResult(Result.success(chat))
            Cancellable.noop
        },
        logger: Logger = mockk(relaxed = true),
        isOnline: Boolean = true,
        onConnected: (ChatStateListener?) -> Cancellable = { Cancellable.noop },
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
            every { build(resultCallback = any()) } answers { _ ->
                val onDone = arg<OnChatBuiltResultCallback?>(0)
                val chat = mockk<Chat>(relaxUnitFun = true) {
                    every { connect() } answers { onConnected(listener) }
                    every { configuration } answers {
                        mockk {
                            every { isAuthorizationEnabled } returns false
                            every { this@mockk.isOnline } returns isOnline
                        }
                    }
                    every { isChatAvailable } returns isOnline
                }

                onBuilt(chat, onDone)
            }
        }
        val provider = ChatInstanceProvider.create(socketFactoryConfiguration, logger = logger) { _, _, _ ->
            builder
        }

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

    @Test(expected = InvalidStateException::class)
    fun prepareThrowsWhenConnected() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.onConnected()

        provider.prepare(applicationContext)
    }

    @Test
    fun configurationAuthenticationRequiredDefaultsFalse() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.configure(applicationContext) {
            assertFalse(authenticationRequired)
        }
    }

    @Test
    fun configureAccessAuthenticationRequired() {
        listOf(true, false).forEach { required ->
            val (provider) = provider(socketFactoryConfiguration)

            provider.prepare(applicationContext)

            val chat = requireNotNull(provider.chat)

            every { chat.configuration.isAuthorizationEnabled } returns required

            provider.configure(applicationContext) {
                assertEquals(required, authenticationRequired)
            }
        }
    }

    @Test
    fun configureSetsConfiguration() {
        val (provider, _) = provider(null)

        provider.configure(applicationContext) {
            configuration = socketFactoryConfiguration
            assertSame(socketFactoryConfiguration, configuration)
        }

        assertSame(provider.configuration, socketFactoryConfiguration)
    }

    @Test
    fun configureSetsUserName() {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = UserName("first name", "last name")

        provider.configure(applicationContext) {
            userName = expect
            assertSame(expect, userName)
        }

        assertEquals(expect, provider.userName)
    }

    @Test
    fun configureSetsLogger() {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = mockk<Logger>()

        provider.configure(applicationContext) {
            logger = expect
            assertSame(expect, logger)
        }

        assertEquals(expect, provider.logger)
    }

    @Test
    fun configureSetsAuthorization() {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = Authorization("code", "verifier")

        provider.configure(applicationContext) {
            authorization = expect
            assertSame(expect, authorization)
        }

        assertEquals(expect, provider.authorization)
    }

    @Test
    fun configureSetsDevelopmentMode() {
        val (provider, _) = provider(socketFactoryConfiguration)
        val expect = !provider.developmentMode

        provider.configure(applicationContext) {
            developmentMode = expect
            assertEquals(expect, developmentMode)
        }

        assertEquals(expect, provider.developmentMode)
    }

    @Test
    fun configureSetsTokenProvider() {
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
    fun configureRestartsChat() {
        val (provider, _) = provider(socketFactoryConfiguration)

        assertNull(provider.chat)

        provider.configure(applicationContext) {}

        val chat = requireNotNull(provider.chat)

        provider.configure(applicationContext) {}

        verify {
            chat.signOut()
        }

        assertNotSame(chat, provider.chat)
        assertEquals(Prepared, provider.chatState)
    }

    @SuppressLint("CheckResult")
    @Test
    fun configureSetsUpBuilder() {
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

        verifyOrder {
            // Note that the order of the sets is not required, it is just required that
            // they all occur before the build call.
            builder.setChatStateListener(provider)
            builder.setDevelopmentMode(false)
            builder.setUserName("first", "last")
            builder.setAuthorization(auth)
            builder.setDeviceToken(token)
            builder.build(resultCallback = any())
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

        verify(ordering = ORDERED) {
            builder.build(resultCallback = any())
            listener.onChatChanged(any())
            listener.onChatStateChanged(Prepared)
        }
    }

    @Test
    fun connectionLostCancels() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
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

        assertEquals(Prepared, provider.chatState)

        provider.cancel()

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun connectedIgnoresCancel() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
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

        provider.prepare(applicationContext)

        verify(exactly = 1) {
            builder.build(resultCallback = any())
        }
    }

    @Test
    fun setUserNameForwardsToChat() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)

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
        val (provider) = provider(
            socketFactoryConfiguration,
            onBuilt = { _, _ -> Cancellable { } }
        )

        provider.prepare(applicationContext)

        assertEquals(Preparing, provider.chatState)

        provider.connect()
    }

    @Test(expected = InvalidStateException::class)
    fun connectThrowsWhenConnecting() {
        val (provider) = provider(socketFactoryConfiguration) {
            Cancellable { }
        }

        provider.prepare(applicationContext)
        provider.connect()
        provider.connect()
    }

    @Test
    fun connectIgnoredWhenConnected() {
        val logger: Logger = mockk(relaxed = true)
        val (provider) = provider(socketFactoryConfiguration, logger = logger)

        provider.prepare(applicationContext)
        provider.onConnected()
        provider.connect()

        verify(exactly = 0) { requireNotNull(provider.chat).connect() }

        verify {
            logger.log(Level.Warning, any(), any())
        }
    }

    @Test
    fun onConnectedAdvancesState() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)

        provider.addListener(listener)
        // this does nothing, but it makes coverage happy
        provider.addListener(object : Listener {})
        provider.prepare(applicationContext)

        provider.onConnected()

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

        provider.onChatRuntimeException(exception)

        verify {
            listener.onChatRuntimeException(exception)
        }
    }

    @Test
    fun closeClosesChat() {
        val (provider) = provider(socketFactoryConfiguration)

        provider.prepare(applicationContext)
        provider.connect()
        val chat = requireNotNull(provider.chat)
        provider.close()

        verify {
            chat.close()
        }

        assertEquals(Prepared, provider.chatState)
    }

    @Test
    fun removeListener() {
        val (provider) = provider(socketFactoryConfiguration)
        val listener = mockk<Listener>(relaxUnitFun = true)

        provider.addListener(listener)
        provider.removeListener(listener)
        provider.onConnected()

        verify(exactly = 0) {
            listener.onChatStateChanged(any())
        }
    }

    @Test
    fun prepareAdvancesStateAndCancels() {
        val cancellable = mockk<Cancellable>(relaxUnitFun = true)
        val (provider) = provider(
            socketFactoryConfiguration,
            onBuilt = { chat, onDone ->
                onDone?.onChatBuiltResult(Result.success(chat))
                cancellable
            }
        )

        provider.prepare(applicationContext)

        assertEquals(Preparing, provider.chatState)

        provider.cancel()

        verify {
            cancellable.cancel()
        }
        assertEquals(Initial, provider.chatState)
    }

    @Test
    fun connectAdvancesStateAndCancels() {
        val cancellable = mockk<Cancellable>(relaxUnitFun = true)
        val (provider) = provider(
            socketFactoryConfiguration,
            onConnected = {
                cancellable
            },
        )

        provider.prepare(applicationContext)
        provider.connect()

        assertEquals(Connecting, provider.chatState)

        provider.cancel()

        verify {
            cancellable.cancel()
        }

        assertEquals(Prepared, provider.chatState)
    }

    @Test(expected = InvalidStateException::class)
    fun reconnectThrowsInInitial() {
        val (provider) = provider(null)

        assertEquals(Initial, provider.chatState)

        @Suppress("DEPRECATION")
        provider.reconnect()
    }

    @Test(expected = InvalidStateException::class)
    fun reconnectThrowsInPreparing() {
        val (provider) = provider(
            socketFactoryConfiguration,
            onBuilt = { _, _ -> Cancellable { } }
        )

        provider.prepare(applicationContext)

        assertEquals(Preparing, provider.chatState)

        @Suppress("DEPRECATION")
        provider.reconnect()
    }

    @Test(expected = InvalidStateException::class)
    fun reconnectThrowsInPrepared() {
        val (provider) = provider(
            socketFactoryConfiguration,
        )

        provider.prepare(applicationContext)

        assertEquals(Prepared, provider.chatState)

        @Suppress("DEPRECATION")
        provider.reconnect()
    }

    @Test(expected = InvalidStateException::class)
    fun reconnectThrowsInConnecting() {
        val (provider) = provider(
            socketFactoryConfiguration,
            onConnected = { _ -> Cancellable { } }
        )

        provider.prepare(applicationContext)
        provider.connect()

        assertEquals(Connecting, provider.chatState)

        @Suppress("DEPRECATION")
        provider.reconnect()
    }

    @Test(expected = InvalidStateException::class)
    fun reconnectThrowsInConnected() {
        val (provider) = provider(
            socketFactoryConfiguration,
        )

        provider.prepare(applicationContext)
        provider.connect()
        provider.onConnected()

        assertEquals(Connected, provider.chatState)
        @Suppress("DEPRECATION")
        provider.reconnect()
    }

    @Test
    fun reconnectConnects() {
        val (provider) = provider(
            socketFactoryConfiguration,
            onConnected = { _ -> Cancellable { } }
        )

        provider.prepare(applicationContext)
        provider.connect()
        provider.onConnected()
        provider.onUnexpectedDisconnect()

        assertEquals(ConnectionLost, provider.chatState)
        @Suppress("DEPRECATION")
        provider.reconnect()

        assertEquals(Connecting, provider.chatState)

        provider.onConnected()

        assertEquals(Connected, provider.chatState)
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
        for(state in ChatState.entries) {
            val provider = ChatInstanceProvider.create(null)
            var thrown = false

            if (state == Initial) {
                provider.advanceState(ConnectionLost)
            }

            @Suppress("SwallowedException")
            try {
                provider.advanceState(state)
            } catch (exc: AssertionError) {
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
        for(state in ChatState.entries) {
            val provider = ChatInstanceProvider.create(null)
            var thrown = false

            if (state == Initial) {
                provider.advanceState(ConnectionLost)
            }

            @Suppress("SwallowedException")
            try {
                provider.advanceState(state, mockk())
            } catch (exc: AssertionError) {
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
        val listener = mockk<ChatInstanceProvider.Listener> {
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
            Cancellable.noop
        }

        provider.prepare(applicationContext)
        provider.connect()

        assertEquals(Offline, provider.chatState)

        provider.cancel()

        assertEquals(Prepared, provider.chatState)
    }

    companion object {
        private const val BRAND_ID = 1000L
        private val CHANNEL_ID = UUID.randomUUID().toString()
        private const val VERSION = "1.0"
    }
}
