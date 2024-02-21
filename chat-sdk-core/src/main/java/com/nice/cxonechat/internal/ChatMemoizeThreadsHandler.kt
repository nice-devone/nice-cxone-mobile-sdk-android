package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadsHandler

/**
 * Implementation of the [ChatWithParameters] which assures that only one instance of [ChatThreadsHandler] is ever
 * created.
 *
 * It memorizes the first instance which is created and prevents further calls to the supplied
 * [ChatWithParameters.threads] method.
 */
internal class ChatMemoizeThreadsHandler(private val origin: ChatWithParameters) : ChatWithParameters by origin {

    private val chatThreadsHandlerMemoized by lazy(origin::threads)

    override fun threads(): ChatThreadsHandler = chatThreadsHandlerMemoized
}
