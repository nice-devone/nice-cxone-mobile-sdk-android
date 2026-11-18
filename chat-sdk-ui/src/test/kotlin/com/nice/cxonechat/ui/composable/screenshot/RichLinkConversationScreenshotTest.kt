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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink
import kotlinx.coroutines.Dispatchers
import org.junit.After
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
 * Full-screen screenshot tests showing a rich link message inside a real [ChatConversation], so the
 * card is captured exactly as an end user sees it in a chat thread (bubble background, spacing, agent
 * avatar, wide-screen padding) rather than in isolation.
 *
 * The rich link's media loads through Coil's [coil3.compose.AsyncImage], which has no network under
 * Robolectric. Each test installs a singleton [ImageLoader] whose engine returns a static [ColorImage]
 * of a chosen size (see [installFakeImage]) and forces the load onto the caller's dispatcher, so the
 * image resolves synchronously during composition and actually renders in the screenshot.
 *
 * Landscape covers both a *tall* image (worst case for the 60%-of-screen height cap — title/URL must
 * stay visible) and a *small* image (the card must wrap to the media size and not stretch to 60%).
 * Portrait covers a *small* image (card wraps to the media, no forced empty space) and a *tall* image
 * that overflows the 60%-of-window-height cap (media crops to fill the remaining height, no distortion).
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RichLinkConversationScreenshotTest {

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
    fun `conversation with rich link portrait light`() {
        installFakeImage(smallImage())
        captureConversation(dark = false)
    }

    @Test
    fun `conversation with rich link portrait dark`() {
        installFakeImage(smallImage())
        captureConversation(dark = true)
    }

    @Test
    fun `conversation with rich link portrait tall image light`() {
        installFakeImage(tallImage())
        captureConversation(dark = false)
    }

    @Test
    fun `conversation with rich link portrait tall image dark`() {
        installFakeImage(tallImage())
        captureConversation(dark = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `conversation with rich link landscape tall image dark`() {
        installFakeImage(tallImage())
        captureConversation(dark = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `conversation with rich link landscape tall image light`() {
        installFakeImage(tallImage())
        captureConversation(dark = false)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `conversation with rich link landscape small image light`() {
        installFakeImage(smallImage())
        captureConversation(dark = false)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `conversation with rich link landscape small image dark`() {
        installFakeImage(smallImage())
        captureConversation(dark = true)
    }

    // A short thread ending in the rich link, so the card is on-screen instead of scrolled off the
    // bottom of a long conversation.
    private fun richLinkThread(): List<SdkMessage> = listOf(
        UiSdkRichLink(
            title = "Check out our latest offer",
            url = "https://nice.com/offer",
            media = PreviewMedia(
                url = "https://example.com/image.jpg",
                mimeType = "image/jpeg",
                fileName = "Preview Image",
            ),
            // Fixed instant so the rendered message timestamp is stable: Clock.System.now() would bake
            // the record-time clock into every baseline and fail them once the wall clock moves.
            createdAt = FIXED_CREATED_AT,
        ),
    )

    // A tall (portrait aspect ratio) image: the worst case for the landscape height cap.
    private fun tallImage(): Image = ColorImage(
        color = Color(0xFF3F51B5).toArgb(),
        width = 600,
        height = 1200,
    )

    // A small image, well below 60% of the screen: the card must wrap to it and not stretch to 60%.
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
            // Resolve synchronously on the caller's thread so the fake image renders during
            // composition under Robolectric (otherwise the card shows only the loading spinner).
            .coroutineContext(EmptyCoroutineContext)
            .mainCoroutineContext(Dispatchers.Main.immediate)
            .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    private fun captureConversation(dark: Boolean) {
        val messages = richLinkThread()
        composeTestRule.setContent {
            ChatTheme(darkTheme = dark) {
                ChatConversation(
                    conversationState = previewUiState(
                        messages = messages,
                        isArchived = false,
                        isLiveChat = false,
                        // Disable the agent-typing indicator: with reverseLayout the indicator is the
                        // bottom item (index 0) and the auto-scroll pins it to the bottom, pushing the
                        // rich link off the short landscape viewport. Without it, the rich link is the
                        // bottom item and stays fully visible.
                        isAgentTyping = false,
                    ),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                )
            }
        }

        // Whichever test runs first pays the one-time Compose/Coil/Robolectric warmup (~40-60s). On
        // that cold frame the LazyColumn items, the async Coil load and the onImageLoaded -> useFillWidth
        // recomposition have not settled, so an immediate capture snapshots the empty "Loading" frame
        // (which previously got recorded as a baseline). waitForIdle/waitUntil alone don't help because
        // this v2 compose rule doesn't auto-advance the paused clock under Robolectric, so we drive the
        // clock manually until the rich-link content is actually laid out, then settle the crossfade.
        composeTestRule.mainClock.autoAdvance = false
        val richLinkNode = composeTestRule.onAllNodesWithTag("rich_link_image", useUnmergedTree = true)
        var elapsedMillis = 0L
        while (richLinkNode.fetchSemanticsNodes().isEmpty() && elapsedMillis < CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.mainClock.advanceTimeBy(FRAME_MILLIS)
            elapsedMillis += FRAME_MILLIS
        }
        // Flush the crossfade animation and any trailing recomposition so the image is fully drawn.
        composeTestRule.mainClock.advanceTimeBy(CROSSFADE_SETTLE_MILLIS)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Gate the capture on the media having actually *loaded*, not merely on the tagged node
        // existing: PresetAsyncImage applies the "rich_link_image" tag to the AsyncImage itself, which
        // is composed and laid out from the first frame, before Coil resolves. The LoadingSpinner is a
        // sibling gated on !imageLoaded, so the spinner being gone is what proves the load completed and
        // onImageLoaded -> useFillWidth has run — the state the baselines actually depend on. Asserted
        // here rather than as the pump loop's exit condition because the load only completes once the
        // clock is un-paused and idle is awaited above.
        val loadingNodes = composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true)
        assertTrue(
            "Rich link image did not finish loading before capture (waited up to ${CAPTURE_TIMEOUT_MILLIS}ms for " +
                "layout plus ${CROSSFADE_SETTLE_MILLIS}ms to settle); refusing to capture a blank frame.",
            richLinkNode.fetchSemanticsNodes().isNotEmpty() && loadingNodes.fetchSemanticsNodes().isEmpty(),
        )

        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        // Fixed message timestamp so the rendered time text is deterministic across runs.
        private val FIXED_CREATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)

        // One rendered frame at 60fps; small enough to land on the exact frame the content appears.
        private const val FRAME_MILLIS = 16L

        // Upper bound for how long we pump frames waiting for the rich-link node to be laid out.
        private const val CAPTURE_TIMEOUT_MILLIS = 5_000L

        // Long enough to finish Coil's crossfade(true) animation before capturing.
        private const val CROSSFADE_SETTLE_MILLIS = 1_000L
    }
}
