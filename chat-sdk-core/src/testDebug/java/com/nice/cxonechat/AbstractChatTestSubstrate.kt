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

import androidx.annotation.CallSuper
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.internal.model.ChannelAvailability
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChannelConfiguration.FileRestrictions
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.ChatEntrailsMock
import com.nice.cxonechat.tool.MockServer
import com.nice.cxonechat.tool.awaitResult
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.Before
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

internal abstract class AbstractChatTestSubstrate {

    protected lateinit var entrails: ChatEntrails
    protected lateinit var service: RemoteService
    protected lateinit var storage: ValueStorage
    protected lateinit var socket: WebSocket
    protected lateinit var proxyListener: ProxyWebSocketListener
    protected lateinit var socketServer: MockServer
    protected var isLiveChat = false
    protected var chatAvailability = Online
    protected var features: MutableMap<String, Boolean> = mutableMapOf()
    protected val httpClient = mockk<OkHttpClient>()

    protected open val config: ChannelConfiguration?
        get() = ChannelConfiguration(
            settings = ChannelConfiguration.Settings(
                hasMultipleThreadsPerEndUser = true,
                isProactiveChatEnabled = true,
                fileRestrictions = FileRestrictions(
                    10,
                    listOf(),
                    false,
                ),
                features = features
            ),
            isAuthorizationEnabled = true,
            preContactForm = null,
            customerCustomFields = listOf(),
            contactCustomFields = listOf(),
            isLiveChat = isLiveChat,
            availability = mockk {
                every { status } answers { chatAvailability }
            }
        )

    @Before
    fun prepareInternal() {
        socketServer = MockServer()
        socket = socketServer.socket
        proxyListener = socketServer.proxyListener
        storage = mockStorage()
        service = mockService()
        entrails = ChatEntrailsMock(httpClient, storage, service, mockLogger(), CXOneEnvironment.EU1.value)
        prepare()
    }

    @CallSuper
    protected abstract fun prepare()

    private fun mockLogger() = object : Logger {
        override fun log(level: Level, message: String, throwable: Throwable?) {
            @Suppress("ProhibitedCall")
            println(message)
            throwable?.printStackTrace()
        }
    }.let(::spyk)

    // relaxUnitFun = true means we don't need to mock all the setters.
    private fun mockStorage(): ValueStorage = mockk(relaxUnitFun = true) {
        every { visitorId } returns UUID.fromString(TestUUID)
        every { customerId } returns TestUUID
        every { destinationId } returns UUID.fromString(TestUUID)
        every { welcomeMessage } returns "welcome"
        every { authToken } returns "token"
        every { authTokenExpDate } returns null
        every { deviceToken } returns null
    }

    fun <T> mockCall(result: () -> T) = mockk<Call<T>> {
        every { execute() } answers { Response.success(result()) }
        every { enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val call = self as Call<T>
            val callback = arg<Callback<T>>(0)

            runCatching { config }
                .onSuccess { callback.onResponse(call, Response.success(result())) }
                .onFailure { callback.onFailure(call, it) }
        }
    }

    private fun mockService() = mockk<RemoteService> {
        every { getChannel(any(), any()) } returns mockCall { config }
        @Suppress("UNCHECKED_CAST")
        every { createOrUpdateVisitor(any(), any(), any()) } returns mockCall { null } as Call<Void>
        every { getChannelAvailability(any(), any()) } returns mockCall {
            ChannelAvailability(status = chatAvailability)
        }
    }

    protected inline fun <T> testCallback(
        body: (trigger: (T) -> Unit) -> Any,
        serverAction: MockServer.() -> Unit,
    ): T = awaitResult(100.milliseconds) {
        body(it).also {
            socketServer.serverAction()
        }
    }

    protected inline fun <T> testCallback(
        body: (trigger: (T) -> Unit) -> Unit,
    ): T = awaitResult(100.milliseconds, body)

    protected infix fun serverResponds(
        message: String,
    ) = socketServer.sendServerMessage(message)

    protected fun assertSendText(
        expected: String,
        vararg except: String,
        replaceDate: Boolean = false,
        expression: () -> Unit,
    ) {
        assertSendTexts(expected, except = except, replaceDate = replaceDate, body = expression)
    }

    protected fun assertSendsNothing(body: () -> Unit) {
        clearMocks(socket)
        every { socket.send(text = any()) } returns true

        body()

        verify(exactly = 0) { socket.send(text = any()) }
    }

    protected fun assertSendTexts(
        vararg expected: String,
        except: Array<out String> = emptyArray(),
        replaceDate: Boolean = false,
        body: () -> Unit,
    ) {
        clearMocks(socket)
        val arguments = mutableListOf<String>()
        every { socket.send(text = capture(arguments)) } returns true

        body()

        assert(arguments.isNotEmpty()) {
            "Nothing was sent to the socket"
        }
        val expectedArray = expected
            .map { if (replaceDate) replaceDate(it, emptyArray()) else it }
            .map { replaceUUID(it, except) }
        arguments
            .map { replaceUUID(it, except) }
            .map { if (replaceDate) replaceDate(it, except) else it }
            .forEachIndexed { index, argument ->
                assertEquals(expectedArray[index], argument)
            }
    }

    protected fun testSendTextFeedback() {
        every { socket.send(text = any()) } returns true
    }

    private fun replaceUUID(text: String, except: Array<out String>): String {
        val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        return uuidPattern.replace(text) { if (it.value in except) it.value else TestUUID }
    }

    private fun replaceDate(text: String, except: Array<out String>): String {
        val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}z")
        return datePattern.replace(text) { if (it.value in except) it.value else "1970-01-01T00:00:00.000z" }
    }

    companion object {
        const val TestContactId = "95vq7qRDsC"
        const val TestUUID = "00000000-0000-0000-0000-000000000000"
        val TestUUIDValue: UUID get() = UUID.fromString(TestUUID)
    }
}
