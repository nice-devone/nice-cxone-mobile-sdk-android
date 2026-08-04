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

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.PreContactCustomFieldDefinitionModel
import com.nice.cxonechat.internal.model.PreContactFormModel
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.SocketFactoryMock
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ChatThreadsHandlerLiveChatPreChatSurveyTest : AbstractLiveChatTest() {

    private val channels = listOf(ChannelIdentifier(SocketFactoryMock.channelId))

    override val config: ChannelConfiguration
        get() = requireNotNull(super.config).copy(
            preContactForm = PreContactFormModel(
                name = "live-chat-pre-chat-test",
                channels = channels,
                customFields = listOf(
                    PreContactCustomFieldDefinitionModel(
                        isRequired = true,
                        definition = Text(fieldId = "text-q", label = "Question"),
                    )
                ),
            )
        )

    @Test
    fun `threadsFlow reports server error when recovering livechat failed with pre-chat survey`() {
        val threads = chat.threads()
        connect()
        val job = testScope.launch { threads.threadsFlow.collect {} }
        socketServer.sendServerMessage(ServerResponse.ErrorResponse(RecoveringLivechatFailed.value))
        job.cancel()
        assertEquals(1, chatStateListener.onChatRuntimeExceptions.size)
        assertTrue(chatStateListener.onChatRuntimeExceptions.all { it is RuntimeChatException.ServerCommunicationError })
    }

    @Test
    fun `threadsFlow emits case closure after thread is created through prechat survey`() {
        val threads = chat.threads()
        connect()

        val emittedLists = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { emittedLists.add(it) } }

        socketServer.sendServerMessage(ServerResponse.LivechatRecovered(status = ContactStatus.Closed))

        val survey = requireNotNull(threads.preChatSurvey)
        val handler = threads.create(
            sequenceOf(
                PreChatSurveyResponse.Text(
                    question = survey.fields.first() as FieldDefinition.Text,
                    response = "test response"
                )
            )
        )

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(handler.get(), Closed))

        job.cancel()

        val lastEmission = emittedLists.last()
        assertEquals(1, lastEmission.size)
        assertFalse(
            lastEmission.first().canAddMoreMessages,
            "Case closure must be reflected in threadsFlow so the prechat survey can be shown again (DE-171093)"
        )
    }

    @Test
    fun `threadsFlow late subscriber receives closed thread from replay after prechat survey thread case-closed`() {
        val threads = chat.threads()
        connect()

        val firstJob = testScope.launch { threads.threadsFlow.collect {} }
        socketServer.sendServerMessage(ServerResponse.LivechatRecovered(status = ContactStatus.Closed))

        val survey = requireNotNull(threads.preChatSurvey)
        val handler = threads.create(
            sequenceOf(
                PreChatSurveyResponse.Text(
                    question = survey.fields.first() as FieldDefinition.Text,
                    response = "test response"
                )
            )
        )
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(handler.get(), Closed))
        firstJob.cancel()

        val lateResults = mutableListOf<List<ChatThread>>()
        val lateJob = testScope.launch { threads.threadsFlow.take(1).toList(lateResults) }

        assertEquals(1, lateResults.size, "Late subscriber must receive one emission from replay")
        assertEquals(1, lateResults[0].size, "Replay must contain the closed thread")
        assertFalse(
            lateResults[0].first().canAddMoreMessages,
            "Replayed thread must have canAddMoreMessages=false so the prechat survey is shown after activity recreation (DE-171094)"
        )

        lateJob.cancel()
    }

    @Test
    fun `create with customFields succeeds after live chat thread case closure`() {
        val threads = chat.threads()
        connect()

        // Subscribe to threadsFlow so ConfigProxy initialises threadCount from -1 to 0.
        val job = testScope.launch { threads.threadsFlow.collect {} }

        socketServer.sendServerMessage(ServerResponse.LivechatRecovered(status = ContactStatus.Closed))

        val survey = requireNotNull(threads.preChatSurvey)
        val surveyField = survey.fields.first() as FieldDefinition.Text
        val handler = threads.create(
            customFields = emptyMap(),
            preChatSurveyResponse = sequenceOf(
                PreChatSurveyResponse.Text(question = surveyField, response = "initial response")
            ),
        )

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(handler.get(), Closed))

        // After case closure, ConfigProxy must allow a new thread (threadCount drops to 0
        // because archived threads are excluded from the active count — DE-171093 regression).
        // If ConfigProxy still counts closed threads, this call throws UnsupportedChannelConfigException.
        threads.create(
            customFields = emptyMap(),
            preChatSurveyResponse = sequenceOf(
                PreChatSurveyResponse.Text(question = surveyField, response = "new response")
            ),
        )

        job.cancel()
    }
}
