@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.READY
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.CLOSED
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Received
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class ChatThreadsHandlerTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() {
            return requireNotNull(super.config).copy(
                contactCustomFields = listOf(
                    Text("testField", "first field"),
                    Text("testField2", "first field")
                )
            )
        }

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test
    fun refresh_sendsExpectedMessage() {
        assertSendText(ServerRequest.FetchThreadList(connection)) {
            threads.refresh()
        }
    }

    @Test
    fun threads_notifies_withInitialList() {
        val expected = List(2) { makeChatThread(threadState = Received) }

        // verify that the metadata is loaded when the list is received
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, expected[0]),
            ServerRequest.LoadThreadMetadata(connection, expected[1])
        ) {
            // Multithread threads should start in READY state.
            assertEquals(READY, chatStateListener.connection)
            val actual = testCallback(::threads) {
                sendServerMessage(ServerResponse.ThreadListFetched(expected))
                sendServerMessage(
                    ServerResponse.ThreadMetadataLoaded(message = makeMessageModel(threadIdOnExternalPlatform = expected[0].id))
                )
                sendServerMessage(
                    ServerResponse.ThreadMetadataLoaded(message = makeMessageModel(threadIdOnExternalPlatform = expected[1].id))
                )
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun create_passesNewThread() {
        val handler = chat.threads().create()
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_withCustomParameters_passesNewThread() {
        val handler = chat.threads().create(emptyMap())
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun threads_notifies_caseClosed() {
        val initial = List(2) { makeChatThread(threadState = Received) }
        val expected = initial.toMutableList().also {
            it[0] = it[0].copy(canAddMoreMessages = false)
        }
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.ThreadListFetched(initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected[0], CLOSED))
        }
        assertNotEquals(expected, initial)
        assertEquals(expected, actual)
    }

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable =
        threads.threads(listener = { listener(it) })
}
