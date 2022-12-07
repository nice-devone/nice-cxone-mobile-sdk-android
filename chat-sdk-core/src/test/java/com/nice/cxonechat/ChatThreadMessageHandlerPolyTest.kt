package com.nice.cxonechat

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.PluginElement
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.MockServer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ChatThreadMessageHandlerPolyTest : AbstractChatTest() {

    private lateinit var handler: ChatThreadHandler
    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
        handler = chat.threads().thread(thread)
    }

    @Test
    fun parses_typeMenu() {
        val message = awaitMessage(ServerResponse.Message.Menu(thread.id))
        assertIs<Message.Plugin>(message)
        assertElementIs<PluginElement.Menu>(message)
    }

    @Test
    fun parses_typeMenu_elements() {
        val message = awaitMessage(ServerResponse.Message.Menu(thread.id))
        assertIs<Message.Plugin>(message)
        val element = message.elements.first()
        assertIs<PluginElement.Menu>(element)
        assertEquals(3, element.buttons.count())
        assertEquals(1, element.texts.count())
        assertEquals(1, element.titles.count())
        assertEquals(1, element.subtitles.count())
        assertEquals(1, element.files.count())
    }

    @Test
    fun parses_typeText() {
        val message = awaitMessage(ServerResponse.Message.Text(thread.id))
        assertIs<Message.Text>(message)
    }

    @Test
    fun parses_typeTextAndButtons() {
        val message = awaitMessage(ServerResponse.Message.TextAndButtons(thread.id))
        assertIs<Message.Plugin>(message)
        val element = message.elements.first()
        assertIs<PluginElement.TextAndButtons>(element)
        assertEquals(3, element.buttons.count())
        assertNotNull(element.text)
    }

    @Test
    fun parses_typeQuickReplies() {
        val message = awaitMessage(ServerResponse.Message.QuickReplies(thread.id))
        assertIs<Message.Plugin>(message)
        val element = message.elements.first()
        assertIs<PluginElement.QuickReplies>(element)
        assertEquals(3, element.buttons.count())
        assertNotNull(element.text)
    }

    @Test
    fun parses_typeInactivityPopup() {
        val message = awaitMessage(ServerResponse.Message.InactivityPopup(thread.id))
        assertIs<Message.Plugin>(message)
        val element = message.elements.first()
        assertIs<PluginElement.InactivityPopup>(element)
        assertNotNull(element.title)
        assertNotNull(element.subtitle)
        assertEquals(2, element.texts.count())
        assertEquals(2, element.buttons.count())
        assertNotNull(element.countdown)
    }

    @Test
    fun parses_typeCustom() {
        val message = awaitMessage(ServerResponse.Message.Custom(thread.id))
        assertIs<Message.Plugin>(message)
        val element = message.elements.first()
        assertIs<PluginElement.Custom>(element)
        assertEquals("See this page", element.fallbackText)
        assertEquals(
            mapOf(
                "color" to "green",
                "buttons" to listOf(
                    mapOf(
                        "id" to "0edc9bf6-4922-4695-a6ad-1bdb248dd42f",
                        "name" to "Confirm"
                    ),
                    mapOf(
                        "id" to "0b4ad5a5-5f6b-477d-8013-d6dcf7b87704",
                        "name" to "Decline"
                    )
                ),
                "size" to mapOf(
                    "ios" to "big",
                    "android" to "middle"
                )
            ), element.variables
        )
    }

    @Test
    fun ignores_unknownContentType() {
        val result = testCallback(::thread) {
            val message = ServerResponse.Message.InvalidContent(thread.id)
            sendServerMessage(ServerResponse.ThreadMetadataLoaded(message))
        }
        assertNull(result)
    }

    @Test
    fun ignores_unknownPluginType() {
        val message = awaitMessage(ServerResponse.Message.InvalidPlugin(thread.id))
        assertIs<Message.Plugin>(message)
        assertEquals(0, message.elements.count())
    }

    // ---

    private inline fun <reified T> assertElementIs(message: Message.Plugin) {
        assertIs<T>(message.elements.first())
    }

    private fun awaitMessage(message: Any, sender: MockServer.() -> Unit = {}): Message {
        return testCallback(::thread) {
            sendServerMessage(ServerResponse.ThreadMetadataLoaded(message))
            sender()
        }.messages.first().also(::println)
    }

    private fun thread(function: (ChatThread) -> Unit): Cancellable {
        return handler.get { function(it) }
    }

}
