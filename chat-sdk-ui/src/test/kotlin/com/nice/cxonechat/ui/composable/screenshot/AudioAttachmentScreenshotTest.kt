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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.storage.DownloadUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.AfterClass
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

/**
 * Verifies the drawn appearance of the inline audio message cell, in a conversation holding one outbound and
 * one inbound one, so a golden covers both alignments and both sides the share button sits on.
 *
 * These run with [LocalInspectionMode] on, which is the only way the cell draws its ready chrome: nothing
 * decodes audio under Robolectric, so a real player would leave the loading placeholder — disabled controls
 * and times formatted from an infinite duration — as the recorded appearance. Unlike the video viewer's
 * placeholder, what is drawn here is the real content, because this cell's chrome is its layout.
 *
 * The landscape and expanded cases are the ones the wide-screen work moves: the cell takes a share of the
 * bubble rather than a fixed width, so its proportion is what these pin.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AudioAttachmentScreenshotTest {

    /** Maps the messages here rather than on a `Dispatchers.Default` thread a loaded runner may not schedule. */
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(dispatcher)

    // Every golden here carries a date header, which is formatted in the default zone and locale.
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    /** [ChatConversation] resolves its configuration through Koin, and throws before drawing anything. */
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

                            // Deprecated upstream, but still abstract, so a fake has to answer it.
                            @Suppress("OVERRIDE_DEPRECATION")
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

    /** The inbound cell carries an avatar, which Coil would otherwise fetch over the network. */
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
                .components { add(engine) }
                .build()
        )
    }

    // The loader is process wide and this module reuses one JVM across test classes, so leaving the stub
    // installed would paint a cyan square into any later test that loads an image.
    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `audio message portrait`() {
        captureConversation()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `audio message portrait dark`() {
        captureConversation(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `audio message landscape`() {
        captureConversation()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `audio message landscape dark`() {
        captureConversation(darkTheme = true)
    }

    /** Right to left, where the cell and the share button beside it have to swap ends. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `audio message landscape right to left`() {
        captureConversation(layoutDirection = LayoutDirection.Rtl)
    }

    /** A window wide enough that the cell must keep the share a landscape phone gives it, not a fixed width. */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `audio message expanded window`() {
        captureConversation()
    }

    private fun captureConversation(
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalInspectionMode provides true,
                ) {
                    ChatConversation(
                        conversationState = previewUiState(
                            messages = listOf(
                                PreviewAttachments.audioSdkMessage(ToClient),
                                PreviewAttachments.audioSdkMessage(ToAgent),
                            ),
                            isArchived = false,
                            isLiveChat = false,
                            // The typing indicator is on by default and would be drawn into every golden.
                            isAgentTyping = false,
                            backgroundDispatcher = dispatcher,
                        ),
                        audioRecordingState = previewAudioState(),
                        onAttachmentTypeSelection = {},
                        onError = {},
                        showMessageProcessing = false,
                    )
                }
            }
        }

        // Settling on a condition, not on waitForIdle(): this cell also waits on the download manager's own
        // thread, which no dispatcher here reaches, so idle can arrive before the cell exists.
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithTag("play_pause_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        /**
         * Insurance rather than a measurement. Mapping the messages runs on the dispatcher the rule drives, so
         * it no longer waits on a [kotlinx.coroutines.Dispatchers.Default] thread a small runner may not spare.
         */
        const val SETTLE_TIMEOUT_MS = 60_000L

        // The download manager is a process-wide singleton, and the conversation asks it for every audio
        // attachment whether or not a player is built. Reset per class rather than per test: nulling the
        // reference makes the next render build another manager, each with its own handler thread over the
        // one cache that nothing resets.
        @JvmStatic
        @AfterClass
        fun resetDownloadManager() = runBlocking { DownloadUtil.resetForTesting() }
    }
}
