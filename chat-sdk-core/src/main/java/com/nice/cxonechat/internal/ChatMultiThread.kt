package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.socket.SocketConnectionListener

/**
 * Handle Multi thread chat specific functionality.
 *
 * A chat in multithread mode is ready once connected.  The process of fetching the thread
 * list and metadata will be initiated once the client indicates an interest in threads by
 * calling [com.nice.cxonechat.Chat.threads]
 *
 * @param origin Existing implementation of [ChatWithParameters] used for delegation.
 */
internal class ChatMultiThread(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin {

    init {
        chatStateListener?.let { listener ->
            socketListener.addListener(
                SocketConnectionListener(listener = listener) {
                    listener.onConnected()
                    listener.onReady()
                }
            )
        }
    }
}
