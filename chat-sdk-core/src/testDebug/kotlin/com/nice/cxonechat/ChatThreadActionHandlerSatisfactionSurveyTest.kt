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
import com.nice.cxonechat.server.ServerResponse.Message.SatisfactionSurvey
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import com.nice.cxonechat.thread.ChatThreadState
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

/**
 * Covers DE-174240: delivering the SATISFACTION_SURVEY popup through the same popupFlow channel
 * inactivity popups already use, including missed-survey recovery and the "deliver at most once
 * per thread" dedup rule (DE-172119 decision #24/#25).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadActionHandlerSatisfactionSurveyTest : AbstractLiveChatTest() {

    private lateinit var thread: ChatThreadMutable
    private lateinit var actions: ChatThreadActionHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread().asMutable()
        actions = chat.threads().thread(thread).actions()
    }

    // ---

    @Test
    fun handler_reports_satisfaction_survey_from_message_created_in_live_chat() = runTest {
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
    fun handler_reports_satisfaction_survey_from_livechat_thread_recover_event() = runTest {
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_reports_satisfaction_survey_from_livechat_thread_recover_event_for_closed_thread() = runTest {
        // A survey is sent precisely when the conversation ends, so by the time it is recovered the
        // thread is normally already archived/closed - unlike InactivityPopup, that must not suppress it.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.LivechatRecovered(
                thread = thread.asCopyable().copy(threadState = ChatThreadState.Closed, canAddMoreMessages = false),
                messages = arrayOf(SatisfactionSurvey(thread.id)),
            )
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(result.await())
    }

    @Test
    fun handler_delivers_satisfaction_survey_only_once_per_thread_live_then_recovery() = runTest {
        // First delivery: live MessageCreated.
        val first = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(first.await())

        // Second encounter for the same thread, via recovery: must not be re-delivered.
        val popup = awaitNoPopup {
            serverResponds(
                ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
            )
        }
        assertNull(popup)
    }

    @Test
    fun handler_delivers_satisfaction_survey_only_once_per_thread_recovery_then_live() = runTest {
        // First delivery: missed-survey recovery.
        val first = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(SatisfactionSurvey(thread.id)))
        )
        advanceUntilIdle()
        assertIs<Popup.SatisfactionSurvey>(first.await())

        // A second message for the same thread must not be re-delivered.
        val popup = awaitNoPopup {
            serverResponds(MessageCreated(thread, SatisfactionSurvey(thread.id)))
        }
        assertNull(popup)
    }

    @Test
    fun handler_drops_malformed_survey_and_still_delivers_a_later_popup() = runTest {
        // A malformed survey (non-HTTPS url) throws while parsing; it must be dropped rather than
        // cancelling the collector below, which would otherwise never see the later, valid message.
        val result = async { actions.popupFlow.first() }
        advanceUntilIdle()
        serverResponds(
            MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "http://survey.example.com/s/bad"))
        )
        advanceUntilIdle()
        serverResponds(
            MessageCreated(thread, SatisfactionSurvey(thread.id, surveyUrl = "https://survey.example.com/s/good"))
        )
        advanceUntilIdle()
        val popup = assertIs<Popup.SatisfactionSurvey>(result.await())
        assertEquals("https://survey.example.com/s/good", popup.surveyUrl)
    }

    // ---

    private suspend fun awaitNoPopup(sender: () -> Unit): Popup? = withTimeoutOrNull(timeMillis = 50) {
        coroutineScope {
            // Started undispatched so the collector is subscribed to the underlying (replay = 0)
            // socket flow before sender() runs - otherwise the event this test sends would be
            // missed entirely, and the assertion would pass regardless of the dedup logic under test.
            val result = async(start = CoroutineStart.UNDISPATCHED) { actions.popupFlow.first() }
            sender()
            result.await()
        }
    }
}
