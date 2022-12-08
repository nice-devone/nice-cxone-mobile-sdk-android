package com.nice.cxonechat

import androidx.annotation.CallSuper
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFrame
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.ChatEntrailsMock
import com.nice.cxonechat.tool.MockServer
import com.nice.cxonechat.tool.awaitResult
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
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
    protected lateinit var socketServer: MockServer

    protected open val config: ChannelConfiguration?
        get() = ChannelConfiguration(
            settings = ChannelConfiguration.Settings(
                hasMultipleThreadsPerEndUser = true,
                isProactiveChatEnabled = true
            ),
            isAuthorizationEnabled = true
        )

    @Before
    fun prepareInternal() {
        socketServer = MockServer()
        socket = socketServer.socket
        storage = mockStorage()
        service = mockService()
        entrails = ChatEntrailsMock(storage, service, mockLogger(), CXOneEnvironment.EU1.value)
        prepare()
    }

    @CallSuper
    protected abstract fun prepare()

    private fun mockLogger() = object : Logger {
        override fun log(level: Level, message: String, throwable: Throwable?) {
            println(message)
            throwable?.printStackTrace()
        }
    }.let(::spy)

    private fun mockStorage(): ValueStorage = mock<ValueStorage>().apply {
        whenever(visitorId).thenReturn(UUID.fromString(TestUUID))
        whenever(consumerId).thenReturn(UUID.fromString(TestUUID))
        whenever(destinationId).thenReturn(UUID.fromString(TestUUID))
        whenever(welcomeMessage).thenReturn("welcome")
        whenever(authToken).thenReturn("token")
    }

    private fun mockService() = mock<RemoteService>().apply {
        val call = mock<Call<ChannelConfiguration?>>()
        whenever(getChannel(any(), any())).thenReturn(call)
        whenever(call.execute()).then { Response.success(config) }
        whenever(call.enqueue(any())).then {
            val callback = it.getArgument<Callback<ChannelConfiguration?>>(0)
            kotlin.runCatching { config }
                .onSuccess { callback.onResponse(call, Response.success(it)) }
                .onFailure { callback.onFailure(call, it) }
            Unit
        }
    }

    protected inline fun <T> testCallback(
        body: (trigger: (T) -> Unit) -> Any,
        serverAction: MockServer.() -> Unit,
    ): T = awaitResult(100.milliseconds) {
        body(it).also {
            serverAction(socketServer)
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

    protected fun assertSendTexts(
        vararg expected: String,
        except: Array<out String> = emptyArray(),
        replaceDate: Boolean = false,
        body: () -> Unit,
    ) {
        val arguments = mutableListOf<String>()
        whenever(socket.sendText(any())).then {
            arguments += it.getArgument<String>(0)
            socketServer.sendAcknowledgement(WebSocketFrame.createTextFrame(arguments.last()))
            socket
        }
        body()
        assert(arguments.isNotEmpty()) {
            "Nothing was sent to the socket"
        }
        arguments
            .map { replaceUUID(it, except) }
            .map { if (replaceDate) replaceDate(it, except) else it }
            .forEachIndexed { index, argument ->
                assertEquals(expected[index], argument)
            }
    }

    protected fun testSendTextFeedback() {
        whenever(socket.sendText(any())).then {
            val text = it.getArgument<String>(0)
            socketServer.sendAcknowledgement(WebSocketFrame.createTextFrame(text))
            socket
        }
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

        const val TestUUID = "00000000-0000-0000-0000-000000000000"
        val TestUUIDValue: UUID get() = UUID.fromString(TestUUID)

    }

}
