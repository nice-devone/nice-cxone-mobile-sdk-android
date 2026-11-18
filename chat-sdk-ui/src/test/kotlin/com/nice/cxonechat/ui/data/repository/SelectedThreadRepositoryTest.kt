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

package com.nice.cxonechat.ui.data.repository

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class SelectedThreadRepositoryTest {

    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val repository = SelectedThreadRepository(
        chatProvider,
        valueStorage = mockk<ValueStorage>(relaxed = true),
        logger = LoggerNoop,
    )

    @Test
    fun `repository registers itself as a provider listener`() {
        verify { chatProvider.addListener(repository) }
    }

    @Test
    fun `session swap re-derives the selected thread handler from the new chat`() {
        val selectedThread = mockk<ChatThread>(relaxed = true)
        val previousSessionHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { get() } returns selectedThread
        }
        repository.chatThreadHandler = previousSessionHandler

        val newSessionHandler = mockk<ChatThreadHandler>(relaxed = true)
        val newSessionChat = mockk<Chat>(relaxed = true) {
            every { threads().thread(selectedThread) } returns newSessionHandler
        }
        repository.onChatChanged(newSessionChat)

        assertSame(
            newSessionHandler,
            repository.chatThreadHandler,
            "a new session's Chat must yield a freshly derived handler for the selected thread"
        )
        assertSame(newSessionHandler, repository.chatThreadHandlerFlow.value)
    }

    @Test
    fun `session swap without a selection keeps NoThreadHandler and derives nothing`() {
        val newSessionChat = mockk<Chat>(relaxed = true)

        repository.onChatChanged(newSessionChat)

        assertSame(NoThreadHandler, repository.chatThreadHandler)
        verify(exactly = 0) { newSessionChat.threads() }
    }

    @Test
    fun `cleared chat resets the selection to NoThreadHandler`() {
        val previousSessionHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { get() } returns mockk(relaxed = true)
        }
        repository.chatThreadHandler = previousSessionHandler

        repository.onChatChanged(null)

        assertSame(NoThreadHandler, repository.chatThreadHandler)
    }

    @Test
    fun `isSatisfactionSurveyShowing stays true until every owner has cleared it`() {
        val ownerA = Any()
        val ownerB = Any()

        repository.setSatisfactionSurveyShowing(ownerA, true)
        repository.setSatisfactionSurveyShowing(ownerB, true)
        assertTrue(repository.isSatisfactionSurveyShowing)

        // A regression to last-writer-wins would make this clear the flag even though B is still showing.
        repository.setSatisfactionSurveyShowing(ownerA, false)
        assertTrue(repository.isSatisfactionSurveyShowing)

        repository.setSatisfactionSurveyShowing(ownerB, false)
        assertFalse(repository.isSatisfactionSurveyShowing)
    }

    @Test
    fun `a survey that fails to persist is still returned by takeSurvey via an in-memory fallback`() = runTest {
        val threadId = UUID.randomUUID()
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
        val failingValueStorage = mockk<ValueStorage>(relaxed = true) {
            coEvery { updateEncryptedPendingSurvey(any()) } throws IOException("disk full")
        }
        val repositoryWithFailingStorage = SelectedThreadRepository(chatProvider, valueStorage = failingValueStorage, logger = LoggerNoop)

        repositoryWithFailingStorage.stashSurvey(threadId, survey)

        assertSame(survey, repositoryWithFailingStorage.takeSurvey(threadId))
    }

    @Test
    fun `a stash write failing after a concurrent sign-out does not resurrect the survey in the fallback`() = runTest {
        val threadId = UUID.randomUUID()
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
        lateinit var repositoryUnderTest: SelectedThreadRepository
        val failingValueStorage = mockk<ValueStorage>(relaxed = true) {
            coEvery { updateEncryptedPendingSurvey(any()) } coAnswers {
                // Sign-out races in while this write is in flight, before it fails.
                repositoryUnderTest.onChatChanged(null)
                throw IOException("disk full")
            }
        }
        repositoryUnderTest = SelectedThreadRepository(chatProvider, valueStorage = failingValueStorage, logger = LoggerNoop)

        repositoryUnderTest.stashSurvey(threadId, survey)

        assertNull(repositoryUnderTest.takeSurvey(threadId))
    }

    @Test
    fun `takeSurvey returns null, not the survey, when the commit removing it fails`() = runTest {
        val threadId = UUID.randomUUID()
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true) {
            every { text } returns "How was it?"
            every { surveyUrl } returns "https://example.test/survey"
        }
        var stored = ""
        var failNextCommit = false
        val flakyValueStorage = mockk<ValueStorage>(relaxed = true) {
            coEvery { updateEncryptedPendingSurvey(any()) } coAnswers {
                // The transform runs (and can capture a value) before this simulated commit
                // failure, mirroring a DataStore write that fails after reading the current value.
                val newValue = firstArg<(String) -> String>()(stored)
                if (failNextCommit) {
                    failNextCommit = false
                    throw IOException("disk full")
                }
                stored = newValue
            }
        }
        val repositoryWithFlakyStorage = SelectedThreadRepository(chatProvider, valueStorage = flakyValueStorage, logger = LoggerNoop)
        repositoryWithFlakyStorage.stashSurvey(threadId, survey)

        failNextCommit = true
        assertNull(repositoryWithFlakyStorage.takeSurvey(threadId))

        // The failed commit never removed the entry, so a later successful call still recovers it.
        val recovered = repositoryWithFlakyStorage.takeSurvey(threadId)
        assertEquals(survey.text, recovered?.text)
        assertEquals(survey.surveyUrl, recovered?.surveyUrl)

        // Now actually removed - a third call finds nothing left to take.
        assertNull(repositoryWithFlakyStorage.takeSurvey(threadId))
    }
}
