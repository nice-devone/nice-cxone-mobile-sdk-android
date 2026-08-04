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

import com.nice.cxonechat.event.thread.ArchiveThreadEvent
import com.nice.cxonechat.exceptions.SdkVersionNotSupported
import com.nice.cxonechat.internal.ChannelConfigurationCache
import com.nice.cxonechat.internal.ChatBuilderInternal
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.nextString
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("RemoveRedundantCallsOfConversionMethods")
internal class ChatBuilderTest : AbstractChatTestSubstrate() {

    private var isAuthorizationEnabled = true

    override val config: ChannelConfiguration?
        get() = super.config?.copy(isAuthorizationEnabled = isAuthorizationEnabled)

    override fun prepare() = Unit

    @After
    fun reset() {
        isAuthorizationEnabled = true
    }

    @Test
    fun build_handlesIOException() {
        var thrownException = false
        val exception = IOException()

        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                if (!thrownException) {
                    thrownException = true
                    throw exception
                } else {
                    Response.success(config)
                }
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val result = build()
        assert(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        val secondResult = build()
        assert(secondResult.isSuccess)
    }

    @Test
    fun build_handlesRuntimeException() {
        var thrownException = false
        val exception = RuntimeException()

        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                @Suppress("TooGenericExceptionThrown")
                if (!thrownException) {
                    thrownException = true
                    throw exception
                } else {
                    Response.success(config)
                }
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val result = build()
        assert(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        val secondResult = build()
        assert(secondResult.isSuccess)
    }

    @Test
    fun build_handlesSdkVersionNotSupported() {
        val errorJson = """
        {
            "error": {
             "errorCode": "SdkVersionNotSupported",
             "errorMessage": "Your version of SDK is not supported anymore, please do upgrade."
             }
         }
    """.trimIndent()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                Response.error(403, errorJson.toResponseBody())
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val result = build()
        assert(result.isFailure)
        assert(result.exceptionOrNull() is SdkVersionNotSupported)
    }

    @Test
    fun build_handles_unknown_field_in_error_response_without_parsing_exception() {
        val errorJson = """
        {
            "error": {
             "errorCode": "InternalError",
             "errorMessage": "This is a new error",
             "transactionId": "${UUID.randomUUID()}",
             "unknownField": "This is a new backend field that the SDK doesn't know about"
             }
         }
    """.trimIndent()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                Response.error(403, errorJson.toResponseBody())
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val result = build()
        assert(result.isFailure)
        assertEquals(
            IllegalStateException::class,
            result.exceptionOrNull()!!::class,
            "Expected IllegalStateException for unknown field in error response, but was ${result.exceptionOrNull()}"
        )
        assert(result.exceptionOrNull()?.message?.contains("Response from the server was not successful") == true)
    }

    @Test
    fun build_handlesFailure() {
        var returnedFailure = false
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                if (!returnedFailure) {
                    returnedFailure = true
                    Response.error(500, "".toResponseBody())
                } else {
                    Response.success(config)
                }
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val result = build()
        assert(result.isFailure)
        val secondResult = build()
        assert(secondResult.isSuccess)
    }

    @Test
    fun build_handlesInvalidBody() {
        var returnedFailure = false
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                if (!returnedFailure) {
                    returnedFailure = true
                    Response.success(null)
                } else {
                    Response.success(config)
                }
            }
        }
        every { service.getChannel(any(), any()) } returns call
        val result = build()
        assert(result.isFailure)
        val secondResult = build()
        assert(secondResult.isSuccess)
    }

    @Test
    fun build_listensTo_welcomeMessage() = runTest {
        val expected = "Welcome, how was your day?"
        val result = build()
        assert(result.isSuccess)
        val connectJob = testScope.launch { result.getOrThrow().connect() }
        socketServer.open()
        socketServer.sendServerMessage(ServerResponse.ConsumerAuthorized())
        connectJob.join()
        this@ChatBuilderTest serverResponds ServerResponse.WelcomeMessage(expected)

        verify { storage.welcomeMessage = expected }
    }

    @Test
    fun build_overrides_username_if_set() {
        val firstName = nextString()
        val lastName = nextString()
        val chat = build {
            setUserName(firstName, lastName)
        }.getOrThrow() as ChatWithParameters
        val connection = chat.connection
        assertEquals(firstName, connection.firstName)
        assertEquals(lastName, connection.lastName)
    }

    @Test
    fun build_keeps_username_if_not_set() {
        val chat = build().getOrThrow() as ChatWithParameters
        val connection = chat.connection
        assertEquals(SocketFactoryMock.firstName, connection.firstName)
        assertEquals(SocketFactoryMock.lastName, connection.lastName)
    }

    @Test
    fun build_persists_deviceToken_if_set() {
        val (_, builder) = prepareBuilder()
        val token = UUID.randomUUID().toString()
        builder.setDeviceToken(token)
        build(builder)

        verify(exactly = 1) { storage.deviceToken = token }
    }

    @Test
    fun build_keeps_deviceToken_if_not_set() {
        val (_, builder) = prepareBuilder()
        build(builder)

        verify(exactly = 0) { storage.deviceToken = any() }
    }

    @Test
    fun build_persists_customerId_if_set_and_clears_settings() {
        val (_, builder) = prepareBuilder()
        val customerId = UUID.randomUUID().toString()
        builder.setCustomerId(customerId)
        build(builder)

        verify(exactly = 1) { storage.customerId = customerId }
    }

    @Test
    fun build_persists_customerId_if_set() = runTest(UnconfinedTestDispatcher()) {
        every { storage.customerId } returns null
        val (_, builder) = prepareBuilder()
        val customerId = UUID.randomUUID().toString()
        builder.setCustomerId(customerId)
        build(builder)

        coVerify(exactly = 0) { storage.clearStorage() }
        verify(exactly = 1) { storage.customerId = customerId }
    }

    @Test
    fun build_keeps_customerId_if_not_set() = runTest(UnconfinedTestDispatcher()) {
        val (_, builder) = prepareBuilder()
        build(builder)

        coVerify(exactly = 0) { storage.clearStorage() }
        verify(exactly = 0) { storage.customerId = any() }
    }

    // ---

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }

    @Test
    fun build_reusesCachedChannelConfiguration_whenPreferred() {
        ChannelConfigurationCache.clear()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } returns Response.success(config)
        }
        every { service.getChannel(any(), any()) } returns call

        // First build fetches the configuration and populates the cache (a provider prepare()).
        assert(build().isSuccess)
        // A per-session rebuild preferring the cache must not repeat the costly getChannel call.
        val rebuild = build {
            (this as? ChatBuilderInternal)?.setPreferCachedConfiguration(true)
            this
        }
        assert(rebuild.isSuccess)
        verify(exactly = 1) { call.execute() }
    }

    @Test
    fun build_fetchesConfigurationAgain_whenCacheNotPreferred() {
        ChannelConfigurationCache.clear()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } returns Response.success(config)
        }
        every { service.getChannel(any(), any()) } returns call

        // Public build() semantics are unchanged: without the internal preference each build fetches.
        assert(build().isSuccess)
        assert(build().isSuccess)
        verify(exactly = 2) { call.execute() }
    }

    private fun build(
        builder: ChatBuilder = prepareBuilder().second,
        body: ChatBuilder.() -> ChatBuilder = { this },
    ): Result<Chat> = kotlinx.coroutines.runBlocking {
        runCatching {
            builder
                .setDevelopmentMode(true)
                .body()
                .build()
        }
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
