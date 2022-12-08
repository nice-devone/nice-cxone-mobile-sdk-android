package com.nice.cxonechat.internal

import com.nice.cxonechat.Chat
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.socket.EventLogger

internal class ChatLogging(
    private val origin: ChatWithParameters,
    logger: Logger,
) : ChatWithParameters by origin, LoggerScope by LoggerScope<Chat>(logger) {

    init {
        finest("Initialized (config=$configuration,environment=$environment)")
        origin.socket.addListener(EventLogger(origin.entrails.logger))
    }

    override fun setDeviceToken(token: String?) = scope("setDeviceToken") {
        duration {
            origin.setDeviceToken(token)
        }
    }

    override fun signOut() = scope("signOut") {
        duration {
            origin.signOut()
        }
    }

    override fun threads() = scope("threads") {
        duration {
            var handler = origin.threads()
            handler = ChatThreadsHandlerLogging(handler, identity)
            handler
        }
    }

    override fun events() = scope("events") {
        duration {
            var handler = origin.events()
            handler = ChatEventHandlerLogging(handler, identity)
            handler
        }
    }

    override fun customFields() = scope("customFields") {
        duration {
            var handler = origin.customFields()
            handler = ChatFieldHandlerLogging(handler, identity)
            handler
        }
    }

    override fun actions() = scope("actions") {
        duration {
            var handler = origin.actions()
            handler = ChatActionHandlerLogging(handler, identity)
            handler
        }
    }

    override fun close() = scope("close") {
        duration {
            origin.close()
        }
    }

}
