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

package com.nice.cxonechat

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatSingleThreadTest : AbstractChatTest() {

    override val config: ChannelConfiguration?
        get() {
            val config = super.config
            return config?.copy(
                settings = config.settings.copy(hasMultipleThreadsPerEndUser = false)
            )
        }

    @Test
    fun chat_is_ready_on_no_thread() {
        prepare()
        assertEquals(ChatStateConnection.INITIAL, chatStateListener.connection)
        serverResponds(ServerResponse.ThreadListFetched(emptyList()))
        assertEquals(ChatStateConnection.READY, chatStateListener.connection)
    }

    @Test
    fun chat_attempts_to_recover_thread() {
        val thread = makeChatThread()
        assertSendTexts(
            ServerRequest.LoadThreadMetadata(connection, thread),
            ServerRequest.RecoverThread(connection, thread),
        ) {
            assertEquals(ChatStateConnection.INITIAL, chatStateListener.connection)
            serverResponds(ServerResponse.ThreadListFetched(listOf(thread)))
            serverResponds(ServerResponse.ThreadMetadataLoaded(message = makeMessageModel(threadIdOnExternalPlatform = thread.id)))
            serverResponds(ServerResponse.ThreadRecovered(thread = thread))
            assertEquals(ChatStateConnection.READY, chatStateListener.connection)
        }
    }
}
