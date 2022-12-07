package com.nice.cxonechat

import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
import org.junit.Test
import org.mockito.kotlin.whenever
import java.util.Date

internal class ChatThreadEventHandlerTest : AbstractChatTest() {

    private lateinit var thread: ChatThread
    private lateinit var events: ChatThreadEventHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
        events = chat.threads().thread(thread).events()
    }

    // ---

    @Test
    fun trigger_sendsExpectedMessage() {
        val event = ChatThreadEvent.Custom { TestModel() }
        assertSendText("""{"field":10}""") {
            events.trigger(event)
        }
    }

    @Test
    fun trigger_refreshesToken() {
        whenever(storage.authTokenExpDate).thenReturn(Date())
        assertSendTexts(
            ServerRequest.RefreshToken(connection),
            """{"field":10}"""
        ) {
            events.trigger(ChatThreadEvent.Custom { TestModel() })
        }
    }

    data class TestModel(
        val field: Int = 10,
    )

}
