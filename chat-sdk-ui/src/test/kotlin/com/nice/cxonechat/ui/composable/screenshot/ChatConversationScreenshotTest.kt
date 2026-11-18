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

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Verifies [ChatConversation] in portrait and landscape, covering the wide-screen horizontal padding. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatConversationScreenshotTest {

    /** Maps the messages here rather than on a `Dispatchers.Default` thread a loaded runner may not schedule. */
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(dispatcher)

    /**
     * Serves every image request a flat colour, because [PreviewMessageProvider]'s rich link points at a
     * live endpoint: unstubbed, the golden records whatever the network returned. Cyan so a stub is obvious.
     *
     * Duplicated in `AttachmentMessageAccessibilityTest`; worth extracting to a shared rule.
     */
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val engine = FakeImageLoaderEngine.Builder()
            .default(ColorImage(Color.CYAN))
            .build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
                .components { add(engine) }
                .build()
        )
    }

    @Before
    fun setUpKoin() {
        startKoin {
            modules(
                module {
                    single<Configuration?> {
                        object : Configuration {
                            override val hasMultipleThreadsPerEndUser = true
                            override val isProactiveChatEnabled = false
                            override val isAuthorizationEnabled = false
                            override val liveChatAllowTranscript = false
                            override val isSecuredCookieEnabled = false
                            override val securedSessions = false
                            override val fileRestrictions = object : FileRestrictions {
                                override val allowedFileSize = 40
                                override val allowedFileTypes: List<FileRestrictions.AllowedFileType> = emptyList()
                                override val isAttachmentsEnabled = false
                            }
                            override val isLiveChat = false
                            override val isOnline = false
                            override fun hasFeature(feature: String) = false
                        }
                    }
                }
            )
        }
    }

    @After
    fun tearDownKoin() {
        stopKoin()
    }

    // The loader is process wide and this module reuses one JVM across test classes, so leaving the stub
    // installed would paint a cyan square into any later test that loads an image.
    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `conversation portrait light`() {
        captureConversation()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `conversation portrait dark`() {
        captureConversation(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `conversation landscape light`() {
        captureConversation()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `conversation landscape dark`() {
        captureConversation(darkTheme = true)
    }

    /**
     * Renders the conversation, then captures it once the messages have been mapped.
     *
     * Idle is enough to settle them, because the mapping runs on [dispatcher] and [composeTestRule] drives that
     * same dispatcher. A conversation carrying audio needs more, the cell there also waiting on the download
     * manager's own thread, which no dispatcher here reaches.
     *
     * Loading more is off: with the messages present from the first frame the list anchors at its top, where
     * the affordance for fetching older ones is drawn, and no fixture here has older ones to fetch.
     *
     * [PreviewMessageProvider] hands out its agents in order, so it is built once per case rather than inside
     * the composition, which would deal a different agent on every recomposition.
     */
    private fun captureConversation(darkTheme: Boolean = false) {
        val messages = PreviewMessageProvider().messages.toList()
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                ChatConversation(
                    conversationState = previewUiState(
                        messages = messages,
                        isArchived = false,
                        isLiveChat = false,
                        canLoadMore = false,
                        backgroundDispatcher = dispatcher,
                    ),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }
}
