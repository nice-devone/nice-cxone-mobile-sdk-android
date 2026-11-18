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

import com.nice.cxonechat.enums.EventType.RecoverThread
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.InactivityPopup
import com.nice.cxonechat.server.ServerResponse.Message.SatisfactionSurvey
import com.nice.cxonechat.server.ServerResponse.Message.Text
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import com.nice.cxonechat.thread.ChatThreadState
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadActionHandlerTest : AbstractMultiThreadChatTest() {
    private lateinit var thread: ChatThreadMutable
    private lateinit var actions: ChatThreadActionHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread().asMutable()
        actions = chat.threads().thread(thread).actions()
    }

    // ---

    @Test
    fun handler_ignores_inactivity_popup_from_message_created() = runTest {
        val popup = awaitNoPopup {
            serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_message_created_in_new_thread() = runTest {
        val newThread = makeChatThread()
        val popup = awaitNoPopup {
            serverResponds(MessageCreated(newThread, InactivityPopup(newThread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event() = runTest {
        val popup = awaitNoPopup {
            serverResponds(ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(InactivityPopup(thread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event_in_new_thread() = runTest {
        val newThread = makeChatThread()
        val popup = awaitNoPopup {
            serverResponds(ServerResponse.ThreadRecovered(thread = newThread, messages = arrayOf(InactivityPopup(newThread.id))))
        }
        assertNull(popup)
    }

    @Test
    fun handler_ignores_inactivity_popup_from_thread_recover_event_for_closed_thread() = runTest {
        val popup = awaitNoPopup {
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
    fun handler_reports_satisfaction_survey_from_message_created() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            MessageCreated(
                thread,
                SatisfactionSurvey(thread.id, text = "How was your experience?", surveyUrl = "https://survey.example.com/s/1"),
            )
        )
        advanceUntilIdle()
        val popup = assertIs<Popup.SatisfactionSurvey>(result.await())
        assertEquals("How was your experience?", popup.text)
        assertEquals("https://survey.example.com/s/1", popup.surveyUrl)
    }

    @Test
    fun handler_reports_missed_satisfaction_survey_from_thread_recover_event() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_reports_missed_satisfaction_survey_from_thread_recover_event_when_not_last_in_list() = runTest {
        // Both test messages share the same fixed createdAt, so maxByOrNull picks the first (the
        // survey) while the lastOrNull it replaced would have picked the second (the popup) instead.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(
                thread = thread,
                messages = arrayOf(SatisfactionSurvey(thread.id), InactivityPopup(thread.id)),
            )
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_reports_missed_satisfaction_survey_from_thread_recover_event_tied_with_a_plain_message() = runTest {
        // Both share the same fixed createdAt. A plain text message converts to a null popup, so
        // picking the max over every message first (instead of over popup candidates only) would
        // let maxByOrNull's first-wins tie-break return that null and silently drop the survey.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(
                thread = thread,
                messages = arrayOf(Text(thread.id), SatisfactionSurvey(thread.id)),
            )
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_reports_missed_satisfaction_survey_from_thread_recover_event_for_closed_thread() = runTest {
        // A survey is sent precisely when the conversation ends, so by the time it is recovered the
        // thread is normally already archived/closed - unlike InactivityPopup, that must not suppress it.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(
                thread = thread.asCopyable().copy(threadState = ChatThreadState.Closed, canAddMoreMessages = false),
                messages = arrayOf(SatisfactionSurvey(thread.id)),
            )
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
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

        assertTrue(sends.any { it.contains(RecoverThread.value) && it.contains(thread.id.toString()) })
        serverResponds(ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id))))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_sends_no_recovery_for_a_pending_thread_that_does_not_exist_on_the_server_yet() = runTest {
        // A Pending thread has no server-side counterpart yet, so recovering it would fail and
        // surface as an error banner - skip the trigger, matching ChatThreadHandlerImpl.refresh().
        val pendingThread = makeChatThread(threadState = ChatThreadState.Pending).asMutable()
        val pendingActions = chat.threads().thread(pendingThread).actions()
        clearMocks(socket)
        val sends = mutableListOf<String>()
        every { socket.send(text = capture(sends)) } returns true

        val job = async(start = CoroutineStart.UNDISPATCHED) { pendingActions.popupFlow.first() }
        advanceUntilIdle()

        assertTrue(sends.none { it.contains(RecoverThread.value) })
        job.cancel()
    }

    @Test
    fun handler_still_delivers_live_popups_when_the_recovery_trigger_fails_to_send() = runTest {
        // A transient failure sending the re-triggered recovery request must not cancel the
        // merged flow and take down the still-valid live MessageCreated popup source with it.
        every { socket.send(text = match { it.contains(RecoverThread.value) }) } returns false

        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_delivers_satisfaction_survey_only_once_per_thread_live_then_recovery() = runTest {
        val first = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(first.await())

        val popup = awaitNoPopup {
            serverResponds(
                ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
            )
        }
        assertNull(popup)
    }

    @Test
    fun handler_delivers_satisfaction_survey_only_once_per_thread_recovery_then_live() = runTest {
        val first = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(first.await())

        val popup = awaitNoPopup {
            serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_drops_malformed_survey_and_still_delivers_a_later_valid_survey() = runTest {
        // A malformed survey must be dropped rather than cancelling the collector below, which
        // would otherwise never see the later valid survey.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "http://survey.example.com/s/bad"))
        )
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "https://survey.example.com/s/good")))
        advanceUntilIdle()
        val popup = assertIs<Popup.SatisfactionSurvey>(result.await())
        assertEquals("https://survey.example.com/s/good", popup.surveyUrl)
    }

    @Test
    fun handler_drops_malformed_survey_from_recovery_and_still_delivers_a_later_valid_survey() = runTest {
        // Same guard as above, for the ThreadRecovered merge branch instead.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.ThreadRecovered(
                thread = thread,
                messages = arrayOf(SatisfactionSurvey(thread.id, surveyUrl = "http://survey.example.com/s/bad")),
            )
        )
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "https://survey.example.com/s/good")))
        advanceUntilIdle()
        val popup = assertIs<Popup.SatisfactionSurvey>(result.await())
        assertEquals("https://survey.example.com/s/good", popup.surveyUrl)
    }

    private suspend fun awaitNoPopup(sender: () -> Unit): Popup? = withTimeoutOrNull(timeMillis = 50) {
        coroutineScope {
            // Started undispatched so the collector is subscribed to the underlying (replay = 0)
            // socket flow before sender() runs - otherwise the event this test sends would be
            // missed entirely, and the assertion would pass regardless of the dedup/filter logic
            // under test.
            val result = async(start = CoroutineStart.UNDISPATCHED) { actions.popupFlow.first() }
            sender()
            result.await()
        }
    }
}
