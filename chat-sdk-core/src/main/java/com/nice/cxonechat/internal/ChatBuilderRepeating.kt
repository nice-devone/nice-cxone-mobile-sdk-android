package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

internal class ChatBuilderRepeating(
    private val origin: ChatBuilder,
    private val entrails: ChatEntrails,
    private val backoff: Duration = 2.seconds,
) : ChatBuilder {

    init {
        check(backoff.inWholeSeconds >= 1) { "Backoff can't be lower than 1 second" }
    }

    override fun setAuthorization(authorization: Authorization) = apply {
        origin.setAuthorization(authorization)
    }

    override fun setDevelopmentMode(enabled: Boolean) = apply {
        origin.setDevelopmentMode(enabled)
    }

    override fun setUserName(first: String, last: String) = apply {
        origin.setUserName(first, last)
    }

    override fun build(callback: OnChatBuiltCallback): Cancellable {
        val threading = entrails.threading
        return threading.background {
            val chat = awaitBuild()
            threading.foreground {
                callback.onChatBuilt(chat)
            }
        }
    }

    // ---

    private fun awaitBuild(): Chat {
        var exponent = 0
        while (true) {
            return try {
                buildSynchronous()
            } catch (ignore: IllegalStateException) {
                val currentBackoff = backoff.toDouble(SECONDS).pow(exponent++).seconds
                Thread.sleep(currentBackoff.inWholeMilliseconds)
                continue
            }
        }
    }

    @Throws(IllegalStateException::class)
    private fun buildSynchronous(): Chat {
        val latch = CountDownLatch(1)
        var chat: Chat? = null
        try {
            origin.build {
                chat = it
                latch.countDown()
            }
        } catch (expected: RuntimeException) {
            throw IllegalStateException(expected)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
        latch.await()
        return checkNotNull(chat)
    }
}
