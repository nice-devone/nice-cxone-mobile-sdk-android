package com.nice.cxonechat.internal

/**
 * Handle Multi thread chat specific functionality.
 *
 * A chat in multithread mode is ready once connected.  The process of fetching the thread
 * list and metadata will be initiated once the client indicates an interest in threads by
 * calling [com.nice.cxonechat.Chat.threads]
 *
 * @param origin Existing implementation of [ChatWithParameters] used for delegation.
 */
internal class ChatMultiThread(private val origin: ChatWithParameters) : ChatWithParameters by origin {
    override fun connect() = origin.connect().also {
        origin.chatStateListener?.onReady()
    }
}
