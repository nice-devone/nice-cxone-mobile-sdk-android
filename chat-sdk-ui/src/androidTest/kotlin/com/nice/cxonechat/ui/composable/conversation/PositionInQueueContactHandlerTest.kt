/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.conversation

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ContactHandler
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import com.nice.cxonechat.ui.domain.model.NoThread
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.util.KoinTestRule
import com.nice.cxonechat.ui.util.fakeConfiguration
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import java.util.UUID

/**
 * Drives the real [ChatThreadViewModel] and [ChatConversation] from a fake [ChatThreadHandler], so the queue overlay
 * rule is checked on a device without a backend.
 */
@OptIn(ExperimentalTestApi::class)
class PositionInQueueContactHandlerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val koinTestRule = KoinTestRule(
        modules = listOf(
            module {
                single<Configuration?> { _ -> fakeConfiguration() }
            }
        )
    )

    private val thread = MutableStateFlow<ChatThread>(
        NoThread.copy(id = UUID.randomUUID(), positionInQueue = 3, contactHandler = null)
    )

    private lateinit var viewModel: ChatThreadViewModel

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        EmojiCompat.init(BundledEmojiCompatConfig(context, Runnable::run))

        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } returns null
        }
        val repository = SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        repository.chatThreadHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { threadFlow } returns thread
            every { get() } answers { thread.value }
        }
        viewModel = ChatThreadViewModel(
            contentDataSource = mockk(relaxed = true),
            selectedThreadRepository = repository,
            chatProvider = chatProvider,
            logger = LoggerNoop,
            sentAttachmentSeeder = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun nonCxoneHandlerHidesOverlayUntilHandlerIsCleared() {
        showConversation()
        waitUntilShown(position = 3)

        update(position = 3, handler = ContactHandler.Cognigy)
        waitUntilHidden()

        // A position update while the bot holds the contact. Absence cannot be awaited; the next step's new number
        // proves the flow moved on. The awaited Cognigy hide above pins the rule.
        update(position = 2, handler = ContactHandler.Cognigy)

        update(position = 4, handler = null)
        waitUntilShown(position = 4)
    }

    @Test
    fun unknownHandlerHidesOverlay() {
        showConversation()
        waitUntilShown(position = 3)

        update(position = 3, handler = ContactHandler.Unknown)
        waitUntilHidden()
    }

    @Test
    fun cxoneHandlerKeepsOverlay() {
        update(position = 3, handler = ContactHandler.CXone)
        showConversation()
        waitUntilShown(position = 3)

        update(position = 5, handler = ContactHandler.CXone)
        waitUntilShown(position = 5)
    }

    private fun showConversation() {
        composeTestRule.setContent {
            ChatConversation(
                conversationState = previewUiState().copy(
                    positionInQueue = viewModel.positionInQueue,
                    isAgentTyping = viewModel.isAgentTyping,
                ),
                audioRecordingState = previewAudioState(),
                onAttachmentTypeSelection = {},
                showMessageProcessing = false,
                onError = {},
                modifier = Modifier,
            )
        }
    }

    private fun update(position: Int, handler: ContactHandler?) {
        thread.value = thread.value.copy(positionInQueue = position, contactHandler = handler)
    }

    private fun waitUntilShown(position: Int) {
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(OVERLAY_TAG), TIMEOUT_MILLIS)
        composeTestRule.waitUntilExactlyOneExists(hasText("You are number $position in line."), TIMEOUT_MILLIS)
    }

    private fun waitUntilHidden() {
        composeTestRule.waitUntilDoesNotExist(hasTestTag(OVERLAY_TAG), TIMEOUT_MILLIS)
    }

    private companion object {
        const val OVERLAY_TAG = "position_in_queue_content_view"
        const val TIMEOUT_MILLIS = 5_000L
    }
}
