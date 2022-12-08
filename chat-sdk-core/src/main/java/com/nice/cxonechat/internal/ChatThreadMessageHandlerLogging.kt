package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.message.ContentDescriptor
import java.util.UUID

internal class ChatThreadMessageHandlerLogging(
    private val origin: ChatThreadMessageHandler,
    logger: Logger,
) : ChatThreadMessageHandler, LoggerScope by LoggerScope<ChatThreadMessageHandler>(logger) {

    override fun loadMore() = scope("loadMore") {
        duration {
            origin.loadMore()
        }
    }

    override fun send(
        message: String,
        listener: OnMessageTransferListener?,
    ) = scope("send(${message.hashCode()})") {
        finest("(message=$message)")
        @Suppress("NAME_SHADOWING")
        val listener = if (listener !is LoggingListener) LoggingListener(listener, this) else listener
        origin.send(message, listener)
    }

    override fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        listener: OnMessageTransferListener?,
    ) = scope("send(${message.hashCode()})") {
        finest("(message=$message,attachments=$attachments)")
        @Suppress("NAME_SHADOWING")
        val listener = if (listener !is LoggingListener) LoggingListener(listener, this) else listener
        origin.send(attachments, message, listener)
    }

    private class LoggingListener(
        private val origin: OnMessageTransferListener?,
        scope: LoggerScope,
    ) : OnMessageTransferListener, LoggerScope by scope {

        override fun onProcessed(id: UUID): Unit = scope("onProcessed") {
            duration {
                origin?.onProcessed(id)
            }
        }

        override fun onSent(id: UUID): Unit = scope("onSent") {
            duration {
                origin?.onSent(id)
            }
        }

    }

}
