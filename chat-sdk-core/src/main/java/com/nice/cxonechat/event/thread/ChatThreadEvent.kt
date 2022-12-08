package com.nice.cxonechat.event.thread

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.Public
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Base class for chat events. It's responsible for generating thread specific
 * models in order to dispatch them to the server.
 *
 * Clients are not permitted to extend this class, though, you might freely use
 * its implementations.
 *
 * @see ChatThreadEventHandler.trigger
 * */
@Public
sealed class ChatThreadEvent {

    internal abstract fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any

    internal class Custom(
        private val factory: (thread: ChatThread) -> Any,
    ) : ChatThreadEvent() {
        override fun getModel(thread: ChatThread, connection: Connection): Any {
            return factory(thread)
        }
    }

}
