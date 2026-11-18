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

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.LocalPdfPageSource
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

/**
 * A PDF attachment inside the message it arrives in: the bubble behind it, the share button beside it, and the
 * two-by-two grid four of them form. What `PdfThumbnailScreenshotTest` cannot show is whether the SDK wraps a
 * page the way the design asks, which is what these are for.
 *
 * The page is stubbed for the reason that class gives. The stub reaches the thumbnail through
 * [LocalPdfPageSource] — one document for the whole conversation, so every tile in a grid shows the same state.
 *
 * The customer's messages only: the sender decides what is behind a tile, and the brand-coloured bubble is the
 * backdrop worth checking. The rendered page is captured in one theme, the stand-in tile in both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfAttachmentMessageScreenshotTest {

    /** These goldens hold the message's own timestamp, so an unpinned zone has to say so rather than diff. */
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    /** A conversation resolves its configuration from Koin as it composes. */
    @get:Rule
    internal val koinRule = ConversationKoinRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `single pdf attachment with a rendered first page`() {
        captureConversation(attachmentCount = 1, renderedPage = stubbedPdfPage())
    }

    @Test
    fun `single pdf attachment before its first page renders`() {
        captureConversation(attachmentCount = 1, renderedPage = null)
    }

    @Test
    fun `four pdf attachments with rendered first pages`() {
        captureConversation(attachmentCount = 4, renderedPage = stubbedPdfPage())
    }

    @Test
    fun `four pdf attachments before their first pages render`() {
        captureConversation(attachmentCount = 4, renderedPage = null)
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun `single pdf attachment before its first page renders dark`() {
        captureConversation(attachmentCount = 1, renderedPage = null, darkTheme = true)
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun `four pdf attachments before their first pages render dark`() {
        captureConversation(attachmentCount = 4, renderedPage = null, darkTheme = true)
    }

    private fun captureConversation(
        attachmentCount: Int,
        renderedPage: Bitmap?,
        darkTheme: Boolean = false,
    ) {
        // Built outside the composition: a fresh instance per recomposition restarts the thumbnail's animation.
        val document = FakePdfPageSource(renderedPage = renderedPage)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalPdfPageSource provides document) {
                ChatTheme(darkTheme = darkTheme) {
                    ChatConversation(
                        conversationState = previewUiState(
                            messages = listOf(
                                UiSdkText(
                                    text = "",
                                    attachments = pdfAttachments(attachmentCount),
                                    direction = ToAgent,
                                    // Fixed, or the record-time clock lands in the golden.
                                    createdAt = FIXED_CREATED_AT,
                                )
                            ),
                            isArchived = false,
                            isLiveChat = false,
                            // A typing indicator is pinned to the bottom, pushing the attachments off the viewport.
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
        awaitMessage(attachmentCount, pageRendered = renderedPage != null)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /**
     * Waits until the message has arrived and its tiles have settled on what they draw.
     *
     * Each state has its own last thing to happen. A stand-in tile resolves its extension off the main thread and
     * is composed showing "N/A" first, so that wait counts resolved labels. A rendered page draws no stand-in tile
     * and so no label, so that wait is for the message the conversation's flow delivers.
     *
     * The spinner assert catches a stub that never arrived: the thumbnail would fall back to opening the
     * attachment, find no Koin graph outside `ChatActivity`, and wait forever.
     */
    private fun awaitMessage(expectedTiles: Int, pageRendered: Boolean) {
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
            if (pageRendered) {
                composeTestRule.onAllNodesWithTag(MESSAGE_TAG, useUnmergedTree = true).fetchSemanticsNodes().size == 1
            } else {
                composeTestRule.onAllNodesWithText(PDF_EXTENSION_LABEL).fetchSemanticsNodes().size == expectedTiles
            }
        }
        assertTrue(
            "A thumbnail is still showing a spinner, so the stubbed document did not reach it.",
            composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    private companion object {
        const val DARK_QUALIFIERS = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger"

        /** Extension the fixture's documents resolve to, drawn on every tile that is still rendering. */
        const val PDF_EXTENSION_LABEL = "PDF"

        /** The whole attachment message, tagged the same way whether it holds one attachment or a grid of them. */
        const val MESSAGE_TAG = "attachment_message"

        val FIXED_CREATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)

        fun pdfAttachments(count: Int): List<Attachment> = List(count) { index ->
            object : Attachment {
                override val url: String = "https://example.com/report_$index.pdf"
                override val friendlyName: String = "report_$index.pdf"
                override val mimeType: String = "application/pdf"
            }
        }
    }
}
