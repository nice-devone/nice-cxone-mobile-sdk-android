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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Instant
import com.nice.cxonechat.message.Message as SdkMessage
import com.nice.cxonechat.ui.util.preview.message.Media as PreviewMedia

/**
 * Pins the rich link card size inside a real [ChatConversation]: one width cap (the bubble's) and a
 * height cap of a third of the window height in portrait, 60% in landscape. Tall images stay cropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class RichLinkLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `a tall image fills a compact card up to the bubble cap`() {
        installFakeImage(tallImage())
        showConversation()
        val card = composeTestRule.onNodeWithTag("rich_link_message").getUnclippedBoundsInRoot()
        assertEquals(246.6f, card.width.value, TOLERANCE) // 0.6 * 411
    }

    @Test
    fun `a tall image is cropped to a third of the window height in a compact window`() {
        installFakeImage(tallImage())
        showConversation()
        val card = composeTestRule.onNodeWithTag("rich_link_message").getUnclippedBoundsInRoot()
        assertEquals(297f, card.height.value, TOLERANCE) // 891 / 3
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a landscape card is at most 60 percent of the content column wide and of the window high`() {
        installFakeImage(tallImage())
        showConversation()
        val card = composeTestRule.onNodeWithTag("rich_link_message").getUnclippedBoundsInRoot()
        assertTrue("width ${card.width} <= 434.4", card.width.value <= 434.4f + TOLERANCE)
        assertTrue("height ${card.height} <= 247.2", card.height.value <= 247.2f + TOLERANCE) // 0.6 * 412
    }

    @Test
    fun `a small image does not stretch the card to the height cap`() {
        installFakeImage(smallImage())
        showConversation()
        val card = composeTestRule.onNodeWithTag("rich_link_message").getUnclippedBoundsInRoot()
        assertTrue("height ${card.height} < 297", card.height.value < 297f)
    }

    private fun richLinkThread(): List<SdkMessage> = listOf(
        UiSdkRichLink(
            title = "Check out our latest offer",
            url = "https://nice.com/offer",
            media = PreviewMedia(
                url = "https://example.com/image.jpg",
                mimeType = "image/jpeg",
                fileName = "Preview Image",
            ),
            createdAt = FIXED_CREATED_AT,
        ),
    )

    private fun tallImage(): Image = ColorImage(
        color = Color(0xFF3F51B5).toArgb(),
        width = 600,
        height = 1200,
    )

    private fun smallImage(): Image = ColorImage(
        color = Color(0xFF4CAF50).toArgb(),
        width = 200,
        height = 170,
    )

    @OptIn(DelicateCoilApi::class, ExperimentalCoilApi::class)
    private fun installFakeImage(image: Image) {
        val engine = FakeImageLoaderEngine(image)
        val imageLoader = ImageLoader.Builder(ApplicationProvider.getApplicationContext())
            .components { add(engine) }
            // Resolve synchronously so the fake image is laid out during composition under Robolectric.
            .coroutineContext(EmptyCoroutineContext)
            .mainCoroutineContext(Dispatchers.Main.immediate)
            .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    private fun showConversation() {
        val messages = richLinkThread()
        composeTestRule.setContent {
            ChatTheme(darkTheme = false) {
                ChatConversation(
                    conversationState = previewUiState(
                        messages = messages,
                        isArchived = false,
                        isLiveChat = false,
                        isAgentTyping = false,
                    ),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                )
            }
        }
        // The compose clock does not auto-advance under Robolectric: pump it until the image is laid out.
        composeTestRule.mainClock.autoAdvance = false
        val imageNode = composeTestRule.onAllNodesWithTag("rich_link_image", useUnmergedTree = true)
        var elapsedMillis = 0L
        while (imageNode.fetchSemanticsNodes().isEmpty() && elapsedMillis < TIMEOUT_MILLIS) {
            composeTestRule.mainClock.advanceTimeBy(FRAME_MILLIS)
            elapsedMillis += FRAME_MILLIS
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    private companion object {
        private val FIXED_CREATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)
        private const val FRAME_MILLIS = 16L
        private const val TIMEOUT_MILLIS = 5_000L
        private const val SETTLE_MILLIS = 1_000L
        private const val TOLERANCE = 1f
    }
}
