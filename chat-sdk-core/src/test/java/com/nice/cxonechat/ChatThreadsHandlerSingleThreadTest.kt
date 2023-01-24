@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextStringMap
import org.junit.Test

internal class ChatThreadsHandlerSingleThreadTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() {
            val config = super.config.let(::requireNotNull)
            return config.copy(settings = config.settings.copy(hasMultipleThreadsPerEndUser = false))
        }

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create()
        }
    }

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_withCustomFields_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            threads {}
            create()
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_withCustomFields_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            threads {}
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create()
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create__withCustomFields_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create(nextStringMap())
    }

    @Test
    fun create_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create()
        }
    }

    @Test
    fun create_withCustomFields_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create(nextStringMap())
        }
    }
}
