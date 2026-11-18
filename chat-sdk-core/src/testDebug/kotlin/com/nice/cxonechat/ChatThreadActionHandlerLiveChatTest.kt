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

import com.nice.cxonechat.enums.EventType.RecoverLivechat
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.InactivityPopup
import com.nice.cxonechat.server.ServerResponse.Message.SatisfactionSurvey
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import com.nice.cxonechat.thread.ChatThreadState
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadActionHandlerLiveChatTest : AbstractLiveChatTest() {

    private lateinit var thread: ChatThreadMutable
    private lateinit var actions: ChatThreadActionHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread().asMutable()
        actions = chat.threads().thread(thread).actions()
    }

    // ---

    @Test
    fun handler_reports_inactivity_popup_from_message_created_in_live_chat() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()
        val popup = result.await()
        assertIs<Popup.InactivityPopup>(popup)
        assertEquals("Lorem Impsum...", popup.body)
        assertEquals("Hello!", popup.title)
    }

    @Test
    fun handler_reports_inactivity_popup_from_livechat_thread_recover_event() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(InactivityPopup(thread.id))))
        advanceUntilIdle()
        val popup = result.await()
        assertIs<Popup.InactivityPopup>(popup)
        assertEquals("Lorem Impsum...", popup.body)
        assertEquals("Hello!", popup.title)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_livechat_thread_recover_event_in_new_thread() = runTest {
        val newThread = makeChatThread()
        val popup = awaitNoPopup {
            serverResponds(ServerResponse.LivechatRecovered(thread = newThread, messages = arrayOf(InactivityPopup(newThread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_livechat_thread_recover_event_for_closed_thread() = runTest {
        val popup = awaitNoPopup {
            serverResponds(
                ServerResponse.LivechatRecovered(
                    thread = thread.asCopyable().copy(threadState = ChatThreadState.Closed, canAddMoreMessages = false),
                    messages = arrayOf(InactivityPopup(thread.id))
                )
            )
        }
        assertNull(popup)
    }

    @Test
    fun handler_requests_a_fresh_recovery_as_soon_as_popupFlow_is_first_collected() = runTest {
        // A late subscriber (e.g. actions() built after connect()'s own recovery already
        // consumed the one-shot event) must not depend on that missed event - it asks again.
        clearMocks(socket)
        val sends = mutableListOf<String>()
        every { socket.send(text = capture(sends)) } returns true

        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()

        assertTrue(sends.any { it.contains(RecoverLivechat.value) && it.contains(thread.id.toString()) })
        serverResponds(ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id))))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_sends_no_recovery_for_a_pending_thread_that_does_not_exist_on_the_server_yet() = runTest {
        // A Pending thread has no server-side counterpart yet, so recovering it would fail and
        // surface as an error banner - skip the trigger, matching ChatThreadHandlerLiveChat.
        val pendingThread = makeChatThread(threadState = ChatThreadState.Pending).asMutable()
        val pendingActions = chat.threads().thread(pendingThread).actions()
        clearMocks(socket)
        val sends = mutableListOf<String>()
        every { socket.send(text = capture(sends)) } returns true

        val job = async(start = CoroutineStart.UNDISPATCHED) { pendingActions.popupFlow.first() }
        advanceUntilIdle()

        assertTrue(sends.none { it.contains(RecoverLivechat.value) })
        job.cancel()
    }

    @Test
    fun handler_still_delivers_live_popups_when_the_recovery_trigger_fails_to_send() = runTest {
        // A transient failure sending the re-triggered recovery request must not cancel the
        // merged flow and take down the still-valid live MessageCreated popup source with it.
        every { socket.send(text = match { it.contains(RecoverLivechat.value) }) } returns false

        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun parses_inactivityPopup() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.InactivityPopup>(result.await())
    }

    @Test
    fun handler_drops_malformed_survey_and_still_delivers_a_later_inactivity_popup() = runTest {
        // A malformed survey (non-HTTPS url) throws while parsing; it must be dropped rather than
        // cancelling the collector below, which would otherwise never see the later inactivity popup.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "http://survey.example.com/s/bad"))
        )
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.InactivityPopup>(result.await())
    }

    @Test
    fun handler_drops_malformed_survey_from_recovery_and_still_delivers_a_later_inactivity_popup() = runTest {
        // Same guard as above, but for the independently-wired LivechatRecovered merge branch in
        // ChatThreadActionHandlerLiveChat - a malformed survey there must not kill the merged flow,
        // which would otherwise also take down handler.popupFlow's sibling MessageCreated source.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.LivechatRecovered(
                thread = thread,
                messages = arrayOf(SatisfactionSurvey(thread.id, surveyUrl = "http://survey.example.com/s/bad")),
            )
        )
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.InactivityPopup>(result.await())
    }

    // ---

    private suspend fun awaitNoPopup(sender: () -> Unit): Popup? {
        sender()
        return withTimeoutOrNull(timeMillis = 50) {
            actions.popupFlow.firstOrNull()
        }
    }
}
