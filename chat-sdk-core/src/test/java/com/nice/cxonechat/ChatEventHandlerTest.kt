package com.nice.cxonechat

import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.server.ServerRequest
import org.junit.Test
import org.mockito.kotlin.whenever
import java.util.Date

internal class ChatEventHandlerTest : AbstractChatTest() {

    private lateinit var events: ChatEventHandler

    override fun prepare() {
        super.prepare()
        events = chat.events()
    }

    // ---

    @Test
    fun trigger_sendExpectedMessage() {
        assertSendText("""{"field":104}""") {
            events.trigger(ChatEvent.Custom { _, _ -> TestValue() })
        }
    }

    @Test
    fun trigger_refreshesToken() {
        whenever(storage.authTokenExpDate).thenReturn(Date())
        assertSendTexts(
            ServerRequest.RefreshToken(connection),
            """{"field":104}"""
        ) {
            events.trigger(ChatEvent.Custom { _, _ -> TestValue() })
        }
    }

    data class TestValue(
        val field: Int = 104,
    )
}
