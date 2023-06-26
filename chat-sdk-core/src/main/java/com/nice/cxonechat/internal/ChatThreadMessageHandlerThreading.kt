package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import java.util.UUID
import kotlin.DeprecationLevel.WARNING

internal class ChatThreadMessageHandlerThreading(
    private val origin: ChatThreadMessageHandler,
    private val chat: ChatWithParameters,
) : ChatThreadMessageHandler by origin {

    @Deprecated(
        message = "Replaced in favor `send(OutboundMessage, OnMessageTransferListener)`",
        replaceWith = ReplaceWith(
            "send(message = OutboundMessage(attachments = files, message = text), listener = listener)"
        ),
        level = WARNING
    )
    override fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        listener: OnMessageTransferListener?,
    ) = send(
        OutboundMessage(
            attachments = attachments,
            message = message,
        ),
        listener
    )

    @Deprecated(
        message = "Replaced in favor `send(OutboundMessage, OnMessageTransferListener)`",
        replaceWith = ReplaceWith("send(message = OutboundMessage(message = text), listener = listener)"),
        level = WARNING
    )
    override fun send(
        message: String,
        listener: OnMessageTransferListener?,
    ) = send(
        OutboundMessage(message),
        listener = listener
    )

    override fun send(
        message: OutboundMessage,
        listener: OnMessageTransferListener?,
    ) {
        chat.entrails.threading.background {
            origin.send(message, listener?.let(::ThreadingListener))
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
