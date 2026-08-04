/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat

import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests the [ChatThreadsHandlerLive] onFailure path where [Configuration.Feature.RecoverLiveChatDoesNotFail]
 * is disabled — meaning a recovery failure triggers thread creation rather than an error report.
 */
internal class ChatThreadsHandlerLiveChatThreadCreationTest : AbstractLiveChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        features[Configuration.Feature.RecoverLiveChatDoesNotFail.key] = false
        super.prepare()
        threads = chat.threads()
    }

    @Test
    fun `threadsFlow emits created thread when recovering livechat failed and RecoverLiveChatDoesNotFail is disabled`() {
        connect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.take(1).toList(results) }
        socketServer.sendServerMessage(ServerResponse.ErrorResponse(RecoveringLivechatFailed.value))
        job.cancel()
        assertEquals(1, results.size, "Should emit once on RecoveringLivechatFailed")
        assertEquals(
            1,
            results[0].size,
            "A new thread should be created when livechat recovery fails and RecoverLiveChatDoesNotFail is disabled"
        )
    }
}
