/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import android.annotation.SuppressLint
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

    @SuppressLint("CheckResult")
    @Test
    fun create_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create()
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun create_withCustomFields_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create(nextStringMap())
        }
    }
}
