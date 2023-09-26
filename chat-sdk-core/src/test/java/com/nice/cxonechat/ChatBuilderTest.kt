/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.awaitResult
import com.nice.cxonechat.tool.nextString
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals

internal class ChatBuilderTest : AbstractChatTestSubstrate() {

    private var isAuthorizationEnabled = true

    override val config: ChannelConfiguration?
        get() = super.config?.copy(
            isAuthorizationEnabled = isAuthorizationEnabled
        )

    override fun prepare() = Unit

    @After
    fun reset() {
        isAuthorizationEnabled = true
    }

    @Test
    fun build_recoversIOException() {
        val call = mock<Call<ChannelConfiguration?>>()
        var thrownException = false
        whenever(service.getChannel(any(), any())).thenReturn(call)
        whenever(call.execute()).then {
            if (!thrownException) {
                thrownException = true
                throw IOException()
            } else {
                Response.success(config)
            }
        }
        build()
    }

    @Test
    fun build_recoversRuntimeException() {
        val call = mock<Call<ChannelConfiguration?>>()
        var thrownException = false
        whenever(service.getChannel(any(), any())).thenReturn(call)
        whenever(call.execute()).then {
            @Suppress("TooGenericExceptionThrown")
            if (!thrownException) {
                thrownException = true
                throw RuntimeException()
            } else {
                Response.success(config)
            }
        }
        build()
    }

    @Test
    fun build_recoversFailure() {
        val call = mock<Call<ChannelConfiguration?>>()
        var returnedFailure = false
        whenever(service.getChannel(any(), any())).thenReturn(call)
        whenever(call.execute()).then {
            if (!returnedFailure) {
                returnedFailure = true
                Response.error(500, "".toResponseBody())
            } else {
                Response.success(config)
            }
        }
        build()
    }

    @Test
    fun build_recoversInvalidBody() {
        val call = mock<Call<ChannelConfiguration?>>()
        var returnedFailure = false
        whenever(service.getChannel(any(), any())).thenReturn(call)
        whenever(call.execute()).then {
            if (!returnedFailure) {
                returnedFailure = true
                Response.success(null)
            } else {
                Response.success(config)
            }
        }
        build()
    }

    @Test
    fun build_authorizesConsumer() {
        val code = "code"
        val verifier = "verifier"
        val (connection, builder) = prepareBuilder()
        assertSendTexts(
            ServerRequest.AuthorizeConsumer(connection, code = code, verifier = verifier),
        ) {
            build(builder) {
                whenever(storage.authToken).thenReturn(null)
                whenever(storage.customerId).thenReturn(null)
                setAuthorization(Authorization(code, verifier))
            }
        }
        verify(service, times(1)).createOrUpdateVisitor(any(), any(), any())
    }

    @Test
    fun build_authorization_updatesConnection() {
        val uuid = UUID.randomUUID()
        val firstName = "new-first-name"
        val lastName = "new-last-name"
        val (connection, builder) = prepareBuilder()
        val chat = build(builder)

        // updates connection
        this serverResponds ServerResponse.ConsumerAuthorized(uuid, firstName, lastName)

        val thread = makeChatThread()
        val expected = connection.asCopyable().copy(customerId = uuid, firstName = firstName, lastName = lastName)
        // tests that data has been updated
        assertSendText(ServerRequest.ArchiveThread(expected, thread), uuid.toString(), thread.id.toString()) {
            chat.threads().thread(thread).events().trigger(ArchiveThreadEvent)
        }
    }

    @Test
    fun build_authorization_updatesStorage_consumer() {
        val uuid = UUID.randomUUID()
        build()
        this serverResponds ServerResponse.ConsumerAuthorized(uuid)
        verify(storage).customerId = uuid
    }

    @Test
    fun build_authorization_updatesStorage_token() {
        val token = nextString()
        build()
        this serverResponds ServerResponse.ConsumerAuthorized(accessToken = token)
        verify(storage).authToken = token
    }

    @Test
    fun build_authorization_updatesStorage_tokenExpDate() {
        val captor = ArgumentCaptor.forClass(Date::class.java)
        build()
        this serverResponds ServerResponse.ConsumerAuthorized()
        verify(storage).authTokenExpDate = captor.capture()
    }

    @Test
    fun build_sets_consumerId_fromStorage() {
        val expected = UUID.randomUUID()
        whenever(storage.customerId).thenReturn(expected)
        val (connection, builder) = prepareBuilder()
        val chat = build(builder)
        val thread = makeChatThread()
        assertSendText(ServerRequest.ArchiveThread(connection, thread), expected.toString(), thread.id.toString()) {
            chat.threads().thread(thread).events().trigger(ArchiveThreadEvent)
        }
    }

    @Test
    fun build_reconnectsConsumer() {
        val (connection, builder) = prepareBuilder()
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
        ) {
            build(builder) {
                whenever(storage.authToken).thenReturn("token")
                this
            }
        }
        verify(service, times(1)).createOrUpdateVisitor(any(), any(), any())
    }

    @Test
    fun build_listensTo_welcomeMessage() {
        val expected = "Welcome, how was your day?"
        build()
        this serverResponds ServerResponse.WelcomeMessage(expected)
        verify(storage).welcomeMessage = expected
    }

    @Test
    fun build_overrides_username_if_set() {
        val firstName = nextString()
        val lastName = nextString()
        val chat = build {
            setUserName(firstName, lastName)
        } as ChatWithParameters
        val connection = chat.connection
        assertEquals(firstName, connection.firstName)
        assertEquals(lastName, connection.lastName)
    }

    @Test
    fun build_keeps_username_if_not_set() {
        val chat = build() as ChatWithParameters
        val connection = chat.connection
        assertEquals(SocketFactoryMock.firstName, connection.firstName)
        assertEquals(SocketFactoryMock.lastName, connection.lastName)
    }

    @Test
    fun build_authorization_keeps_local_username_for_config_without_enabled_authorization() {
        isAuthorizationEnabled = false
        val firstName = nextString()
        val lastName = nextString()
        val uuid = UUID.randomUUID()
        val empty = ""
        val (connection, builder) = prepareBuilder()
        val chat = build(builder) {
            setUserName(firstName, lastName)
        } as ChatWithParameters

        // updates connection
        this serverResponds ServerResponse.ConsumerAuthorized(uuid, empty, empty)

        val thread = makeChatThread()
        val expected = connection.asCopyable().copy(customerId = uuid, firstName = firstName, lastName = lastName)
        // tests that data has been updated
        assertSendText(ServerRequest.ArchiveThread(expected, thread), uuid.toString(), thread.id.toString()) {
            chat.threads().thread(thread).events().trigger(ArchiveThreadEvent)
        }
    }

    @Test
    fun build_persists_deviceToken_if_set() {
        val (_, builder) = prepareBuilder()
        val token = UUID.randomUUID().toString()
        builder.setDeviceToken(token)
        build(builder)
        verify(storage, times(1)).deviceToken = token
    }

    @Test
    fun build_keeps_deviceToken_if_not_set() {
        val (_, builder) = prepareBuilder()
        build(builder)
        verify(storage, times(0)).deviceToken = any()
    }

    // ---

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }

    private fun build(
        builder: ChatBuilder = prepareBuilder().second,
        body: ChatBuilder.() -> ChatBuilder = { this },
    ): Chat = awaitResult {
        builder
            .setDevelopmentMode(true)
            .body()
            .build(it)
    }
}
