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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
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
import kotlin.time.Instant

/**
 * Appearance of a sent message carrying file attachments, captured inside a real [ChatConversation] so the
 * bubble, the share button beside it and the wide-screen content inset are all in frame.
 *
 * Files rather than images throughout: a file tile is drawn from a local icon and the file's extension, so
 * these goldens hold no decoded bitmap and need no image loader stubbed. It also keeps them on the subject —
 * the tiles' size and the grid they form, which is what a wide window changes.
 *
 * The counts are one attachment (a single tile, drawn without a bubble behind it) and four (the full
 * two-by-two grid). Right-to-left is captured on the customer's four-tile case, whose grid and share button
 * exercise everything the single one does.
 *
 * Both senders are captured, because a tile's backdrop depends on which one sent it: the customer's bubble
 * is the brand colour, the agent's is a neutral surface, and a single attachment makes the bubble
 * transparent for either. Landscape is captured for the customer only — the sender changes the colours
 * behind the tile, not its geometry, and geometry is the only thing the window changes.
 *
 * The overflow case - a fifth attachment, which blurs the fourth tile under a `+N` badge - is deliberately
 * not captured. `Modifier.blur` does not render the same pixels on every host, so a golden of it passes only
 * where it was recorded: captured here, the two overflow images agreed on macOS and differed on CI's Linux
 * runner by far more than the shared tolerance allows, and no tolerance wide enough to absorb the blur would
 * still notice a component moving a few dp. Its behaviour is pinned instead by `AttachmentMessageLayoutTest`,
 * which asserts the tile count, the badge and its label without depending on how the blur is drawn.
 *
 * On the agent's messages the glyph reads faintly, which is the design rather than a capture that went wrong.
 * The tile's frame and glyph share one tone, `Brand/On Primary`, and the agent's backdrop is a near-white
 * surface, so in the light theme a white glyph sits on it: the frame is what marks the tile out, and the
 * extension keeps its own brand pill. The customer's brand backdrop gives the same tone full contrast.
 *
 * `AttachmentMessageLayoutTest` pins the measurements behind these; a tile is a rounded square at any size,
 * so a golden cannot say which size it was given.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentMessageScreenshotTest {

    /** These goldens hold the message's own timestamp, so an unpinned zone has to say so rather than diff. */
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

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

    @Test
    fun `single attachment file portrait light`() {
        captureConversation(fileCount = 1)
    }

    @Test
    fun `four attachment files portrait light`() {
        captureConversation(fileCount = 4)
    }

    @Test
    @Config(qualifiers = PORTRAIT_DARK_QUALIFIERS)
    fun `single attachment file portrait dark`() {
        captureConversation(fileCount = 1, darkTheme = true)
    }

    @Test
    @Config(qualifiers = PORTRAIT_DARK_QUALIFIERS)
    fun `four attachment files portrait dark`() {
        captureConversation(fileCount = 4, darkTheme = true)
    }

    @Test
    @Config(qualifiers = WIDE_LIGHT_QUALIFIERS)
    fun `single attachment file landscape light`() {
        captureConversation(fileCount = 1)
    }

    @Test
    @Config(qualifiers = WIDE_LIGHT_QUALIFIERS)
    fun `four attachment files landscape light`() {
        captureConversation(fileCount = 4)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `four attachment files landscape dark`() {
        captureConversation(fileCount = 4, darkTheme = true)
    }

    /**
     * The bubble and its share button swap sides together, so the gap between them stays on the inner edge
     * rather than collapsing against the window.
     */
    @Test
    @Config(qualifiers = WIDE_LIGHT_QUALIFIERS)
    fun `four attachment files landscape right to left`() {
        captureConversation(fileCount = 4, layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun `single attachment file from the agent portrait light`() {
        captureConversation(fileCount = 1, direction = ToClient)
    }

    @Test
    fun `four attachment files from the agent portrait light`() {
        captureConversation(fileCount = 4, direction = ToClient)
    }

    @Test
    @Config(qualifiers = PORTRAIT_DARK_QUALIFIERS)
    fun `single attachment file from the agent portrait dark`() {
        captureConversation(fileCount = 1, direction = ToClient, darkTheme = true)
    }

    @Test
    @Config(qualifiers = PORTRAIT_DARK_QUALIFIERS)
    fun `four attachment files from the agent portrait dark`() {
        captureConversation(fileCount = 4, direction = ToClient, darkTheme = true)
    }

    private fun captureConversation(
        fileCount: Int,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        direction: MessageDirection = ToAgent,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ChatTheme(darkTheme = darkTheme) {
                    ChatConversation(
                        conversationState = previewUiState(
                            messages = listOf(
                                UiSdkText(
                                    text = "",
                                    attachments = files(fileCount),
                                    direction = direction,
                                    // Fixed instant so the rendered timestamp is stable: Clock.System.now()
                                    // would bake the record-time clock into every golden.
                                    createdAt = FIXED_CREATED_AT,
                                )
                            ),
                            isArchived = false,
                            isLiveChat = false,
                            // The typing indicator is the bottom item under reverseLayout, and the auto-scroll
                            // pins it there, which pushes the attachments off the short landscape viewport.
                            isAgentTyping = false,
                        ),
                        audioRecordingState = previewAudioState(),
                        onAttachmentTypeSelection = {},
                        onError = {},
                        showMessageProcessing = false,
                    )
                }
            }
        }
        awaitTiles(fileCount)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /**
     * Waits until all [expectedTiles] tiles carry their file extension rather than the placeholder they
     * start on.
     *
     * Each tile resolves its own extension off the main thread, so it is first composed showing
     * `fallback_document_initial_extension` — which is "N/A", a string the tile draws like any other — and
     * only then the real one. `waitForIdle()` cannot separate the two, since the placeholder frame is idle
     * in its own right, and waiting for the first resolved label would leave the rest free to be captured
     * mid-flight and bake "N/A" into a golden. Counting them is what makes the capture deterministic; it
     * also proves the messages arrived through the conversation's own flow.
     */
    private fun awaitTiles(expectedTiles: Int) {
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(FILE_EXTENSION_LABEL).fetchSemanticsNodes().size == expectedTiles
        }
    }

    private companion object {
        const val WIDE_LIGHT_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** The class qualifier at night, shared by the dark portrait cases. */
        const val PORTRAIT_DARK_QUALIFIERS = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger"

        /** Extension the fixture's files resolve to, drawn on every tile. */
        const val FILE_EXTENSION_LABEL = "TXT"

        val FIXED_CREATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)

        fun files(count: Int): List<Attachment> = List(count) { index ->
            object : Attachment {
                override val url: String = "https://example.com/report_$index.txt"
                override val friendlyName: String = "report_$index.txt"
                override val mimeType: String = "text/plain"
            }
        }
    }
}
