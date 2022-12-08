package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.message.ContentDescriptor
import java.util.UUID

internal class ChatThreadMessageHandlerThreading(
    private val origin: ChatThreadMessageHandler,
    private val chat: ChatWithParameters,
) : ChatThreadMessageHandler by origin {

    override fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        listener: OnMessageTransferListener?,
    ) {
        chat.entrails.threading.background {
            origin.send(attachments, message, listener?.let(::ThreadingListener))
        }
    }

    private inner class ThreadingListener(
        private val listener: OnMessageTransferListener,
    ) : OnMessageTransferListener {

        override fun onProcessed(id: UUID) {
            chat.entrails.threading.foreground {
                listener.onProcessed(id)
            }
        }

        override fun onSent(id: UUID) {
            chat.entrails.threading.foreground {
                listener.onSent(id)
            }
        }

    }

}
