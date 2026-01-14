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

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.InactivityPopup
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.tool.MockServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class ChatThreadActionHandlerTest : AbstractChatTest() {
    private lateinit var thread: ChatThreadMutable
    private lateinit var actions: ChatThreadActionHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread().asMutable()
        actions = chat.threads().thread(thread).actions()
    }

    @AfterTest
    fun after() {
        isLiveChat = false
    }

    // ---

    @Test
    fun handler_reports_inactivity_popup_from_message_created_in_live_chat() {
        setupLiveChat()
        val popup = awaitAction {
            serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        }
        assertIs<Popup.InactivityPopup>(popup)
        assertEquals("Lorem Impsum...", popup.body)
        assertEquals("Hello!", popup.title)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_message_created() {
        val popup = awaitAction {
            serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_message_created_in_new_thread() {
        val newThread = makeChatThread()
        val popup = awaitAction {
            serverResponds(MessageCreated(newThread, InactivityPopup(newThread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event() {
        val popup = awaitAction {
            serverResponds(ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(InactivityPopup(thread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_reports_inactivity_popup_from_livechat_thread_recover_event() {
        setupLiveChat()
        val popup = awaitAction {
            serverResponds(ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(InactivityPopup(thread.id))))
        }
        assertIs<Popup.InactivityPopup>(popup)
        assertEquals("Lorem Impsum...", popup.body)
        assertEquals("Hello!", popup.title)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event_in_new_thread() {
        val newThread = makeChatThread()
        val popup = awaitAction {
            serverResponds(ServerResponse.ThreadRecovered(thread = newThread, messages = arrayOf(InactivityPopup(newThread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_livechat_thread_recover_event_in_new_thread() {
        setupLiveChat()
        val newThread = makeChatThread()
        val popup = awaitAction {
            serverResponds(ServerResponse.LivechatRecovered(thread = newThread, messages = arrayOf(InactivityPopup(newThread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event_for_closed_thread() {
        val popup = awaitAction {
            serverResponds(
                ServerResponse.ThreadRecovered(
                    thread = thread.asCopyable().copy(threadState = ChatThreadState.Closed, canAddMoreMessages = false),
                    messages = arrayOf(InactivityPopup(thread.id))
                )
            )
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_livechat_thread_recover_event_for_closed_thread() {
        setupLiveChat()
        val popup = awaitAction {
            serverResponds(
                ServerResponse.LivechatRecovered(
                    thread = thread.asCopyable().copy(threadState = ChatThreadState.Closed, canAddMoreMessages = false),
                    messages = arrayOf(InactivityPopup(thread.id))
                )
            )
        }
        assertNull(popup)
    }

    private fun setupLiveChat() {
        isLiveChat = true
        prepare()
    }

    private fun awaitAction(sender: MockServer.() -> Unit = {}): Popup {
        val body: ((Popup) -> Unit) -> Unit = { onPopup ->
            actions.onPopup { popup -> onPopup(popup) }
        }
        return testCallback(body) {
            sender()
        }.also(::println)
    }
}