/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.ChatStateListener
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

    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = apply {
        origin.setChatStateListener(listener)
    }

    override fun setDeviceToken(token: String): ChatBuilder = apply {
        origin.setDeviceToken(token)
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
        @Suppress("UnconditionalJumpStatementInLoop") // We need to retry, since we don't have return value
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
