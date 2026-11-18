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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.NoExtraCustomFields
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.composable.PreviewThread
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.screen.ThreadListScreen
import com.nice.cxonechat.ui.util.preview.message.Text
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

/**
 * Screenshot coverage for [ThreadListScreen] in portrait and landscape, light and dark.
 *
 * The thread list itself has no threads, since the point of these goldens is the screen's own
 * layout (top bar, FAB, and the content box's padding) rather than row content, which is already
 * covered by [ThreadListWideScreenScreenshotTest] and [ThreadListAvatarScreenshotTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThreadListScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUpKoin() {
        startKoin {
            modules(
                module {
                    single<Logger>(named(UiModule.LOGGER_NAME)) { LoggerNoop }
                    single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) { NoExtraCustomFields }
                    single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Contact)) { NoExtraCustomFields }
                    single<ChatThreadsViewModel> { chatThreadsViewModel() }
                }
            )
        }
    }

    @After
    fun tearDownKoin() {
        stopKoin()
    }

    @Test
    fun `thread list screen portrait light`() {
        captureThreadListScreen()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `thread list screen portrait dark`() {
        captureThreadListScreen(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `thread list screen landscape light`() {
        captureThreadListScreen()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `thread list screen landscape dark`() {
        captureThreadListScreen(darkTheme = true)
    }

    private fun captureThreadListScreen(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                ThreadListScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    navController = rememberNavController(),
                    modifier = Modifier,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /** A [ChatThreadsViewModel] backed by a connected [Chat] mock with a few real threads. */
    private fun chatThreadsViewModel(): ChatThreadsViewModel {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }
        val threadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        threadsFlow.tryEmit(threads())
        val handler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.threadsFlow } returns threadsFlow
        }
        val configuration = mockk<Configuration> {
            every { hasMultipleThreadsPerEndUser } returns true
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns handler
            every { this@mockk.configuration } returns configuration
        }
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns chat
        }
        return ChatThreadsViewModel(
            chatProvider,
            SelectedThreadRepository(chatProvider, valueStorage = mockk(relaxed = true), logger = LoggerNoop),
            chatStateViewModel,
            mockk(relaxed = true),
        )
    }

    // Three real threads with last-message text, so the rows and dividers reach the horizontal
    // inset boundary and a missing/duplicated inset is visible in the golden.
    private fun threads(): List<ChatThread> = List(THREAD_COUNT) { index ->
        val unread = index == 0
        PreviewThread(
            threadName = "Conversation ${index + 1}",
            threadAgent = agent(firstName = "Agent", lastName = "${index + 1}"),
            messages = listOf(
                Text(
                    text = "Last message in conversation ${index + 1}",
                    direction = if (unread) ToClient else ToAgent,
                    createdAt = FIXED_MESSAGE_TIME,
                ),
            ),
        )
    }

    private fun agent(firstName: String?, lastName: String?): Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = firstName
        override val lastName: String? = lastName
        override val nickname: String? = null
        override val isBotUser: Boolean = false
        override val isSurveyUser: Boolean = false
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private companion object {
        const val THREAD_COUNT = 3

        // Fixed message timestamp so the rendered time text is deterministic across runs.
        val FIXED_MESSAGE_TIME = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}
