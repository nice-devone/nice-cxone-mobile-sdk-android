@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.ChatThreadEventHandlerActions.archiveThread
import com.nice.cxonechat.ChatThreadEventHandlerActions.markThreadRead
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingEnd
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingStart
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
import org.junit.Test

internal class ChatThreadEventTest : AbstractChatTest() {

    private lateinit var events: ChatThreadEventHandler
    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
        events = chat.threads().thread(thread).events()
    }

    // ---

    @Test
    fun trigger_ArchiveThreadEvent_sendsExpectedMessage() {
        val id = thread.id
        assertSendText(ServerRequest.ArchiveThread(connection, thread), id.toString()) {
            events.archiveThread()
        }
    }

    @Test
    fun trigger_MarkThreadReadEvent_sendsExpectedMessage() {
        val id = thread.id
        assertSendText(ServerRequest.MarkThreadRead(connection, thread), id.toString()) {
            events.markThreadRead()
        }
    }

    @Test
    fun trigger_TypingStartEvent_sendsExpectedMessage() {
        val id = thread.id
        assertSendText(ServerRequest.SenderTypingStarted(connection, thread), id.toString()) {
            events.typingStart()
        }
    }

    @Test
    fun trigger_TypingEndEvent_sendsExpectedMessage() {
        val id = thread.id
        assertSendText(ServerRequest.SenderTypingEnded(connection, thread), id.toString()) {
            events.typingEnd()
        }
    }

}
