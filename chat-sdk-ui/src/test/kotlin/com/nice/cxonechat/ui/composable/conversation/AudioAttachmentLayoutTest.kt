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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.storage.DownloadUtil
import com.nice.cxonechat.ui.util.ChatConfigurationRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the geometry the audio message cell takes in each window class.
 *
 * Every case renders under [LocalInspectionMode], which is the only way the cell reaches its ready chrome
 * here: Robolectric decodes no media, so a real player never leaves its loading state and there would be
 * nothing laid out to measure. Appearance is covered by `AudioAttachmentScreenshotTest` instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class AudioAttachmentLayoutTest {

    /** Maps the messages here rather than on a `Dispatchers.Default` thread a loaded runner may not schedule. */
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(dispatcher)

    @get:Rule
    val chatConfiguration = ChatConfigurationRule()

    /**
     * Both assertions separate the ready chrome from the loading placeholder, which is what makes this a
     * test of the seam rather than of the tags: the placeholder renders the same `play_pause_button` and
     * `progress_indicator`, only disabled, and hides the seek buttons behind `canSeekBack` / `canSeekForward`.
     */
    @Test
    fun `inspection mode renders the ready chrome without a player`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ChatTheme {
                    AudioAttachment(
                        attachment = Message.AudioAttachment(
                            message = PreviewAttachments.audioSdkMessage(ToClient),
                            attachment = PreviewAttachments.sound,
                        )
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("play_pause_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed()
    }

    /**
     * The cell is measured inside the real conversation, because its width comes from the bubble cap
     * `MessageFrame` derives from [androidx.compose.ui.platform.LocalWindowInfo] — rendering [AudioAttachment]
     * on its own measures the window instead, and would pass whatever the cell did.
     *
     * `411 × 0.6` caps the bubble at 246.6dp, and the cell fills it.
     */
    @Test
    fun `a compact window fills the bubble with the audio cell`() {
        showConversation()

        assertEquals(246.6f, audioCellWidth(), TOLERANCE)
    }

    /** `(852 − 2 × 64) × 0.6` caps the bubble at 434.4dp, and the cell fills it. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a landscape window fills the bubble with the audio cell`() {
        showConversation()

        assertEquals(434.4f, audioCellWidth(), TOLERANCE)
    }

    /** `(1280 − 2 × 64) × 0.6` caps the bubble at 691.2dp, and the cell fills it. */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `an expanded window fills the bubble with the audio cell`() {
        showConversation()

        assertEquals(691.2f, audioCellWidth(), TOLERANCE)
    }

    /**
     * The bubble alone leaves the cell too narrow for its own controls on a narrow window, and the cell clips
     * rather than wraps, so the loss is silent. `320 × 0.6` gives 192dp against the row's 216dp.
     */
    @Test
    @Config(qualifiers = "w320dp-h740dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a narrow window keeps the audio cell wide enough for its playback row`() {
        showConversation()

        assertEquals(PLAYBACK_ROW_CELL_WIDTH, audioCellWidth(), TOLERANCE)
    }

    /**
     * The iOS design's 20dp gap across full touch targets: `48 + 20`. Both gaps, because one is evidence for the
     * arrangement only if the row is symmetric.
     */
    @Test
    fun `the playback icons sit 68dp apart`() {
        showConversation()

        assertEquals(68f, centreX("play_pause_button") - centreX("seek_back_button"), TOLERANCE)
        assertEquals(68f, centreX("seek_forward_button") - centreX("play_pause_button"), TOLERANCE)
    }

    /**
     * The design draws the play glyph at 48dp against the seek glyphs' 34dp, so it reads as the primary control.
     *
     * Material 3 sizes an icon button's container at 40dp and clips to it, so asking the icon inside one for more
     * than 40dp is silently ignored — which inverts the design, the seek glyphs ending up the largest thing in the
     * row. Measured on the glyph's own node rather than the button's, the button already being 48dp either way.
     */
    @Test
    fun `the play glyph fills the 48dp the design gives it`() {
        showConversation()

        val glyph = composeTestRule
            .onAllNodesWithContentDescription(playDescription(), useUnmergedTree = true)[0]
            .getUnclippedBoundsInRoot()

        assertEquals(48f, glyph.width.value, TOLERANCE)
        assertEquals(48f, glyph.height.value, TOLERANCE)
    }

    private fun playDescription(): String =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.content_description_play)

    /** The one internal measurement the portrait and landscape frames agree on, against `space.medium`'s 8dp in the code. */
    @Test
    fun `the progress bar is 6dp tall`() {
        showConversation()

        val measured = audioNode("progress_indicator").height.value

        assertEquals(6f, measured - PROGRESS_SEMANTICS_INFLATION, TOLERANCE)
    }

    /**
     * The design insets the cell by 16dp on all four sides, but draws its 48dp play glyph overflowing the 28dp
     * row holding it, leaving 6dp of visible clearance below. Compose gives the button its full height instead of
     * letting it overflow, so the bottom inset here is the design's visible one rather than its declared one.
     *
     * `audio_player` tags the cell's content, inside the padding, while `audio_attachment` wraps the padded cell,
     * so the difference between the two is the padding itself — measured directly rather than through a glyph,
     * which would fold in the gap between a button's box and what it paints.
     */
    @Test
    fun `the cell insets its content by 16dp, and by 6dp below the playback row`() {
        showConversation()

        val cell = audioNode("audio_attachment")
        val content = audioNode("audio_player")

        assertEquals(16f, content.left.value - cell.left.value, TOLERANCE)
        assertEquals(16f, cell.right.value - content.right.value, TOLERANCE)
        assertEquals(16f, content.top.value - cell.top.value, TOLERANCE)
        assertEquals(6f, cell.bottom.value - content.bottom.value, TOLERANCE)
    }

    /**
     * The design puts its 64dp margin on the avatar rather than on the bubble, and the avatar is drawn half
     * outside the bubble's leading edge. `Modifier.offset` reserves no room for that, so the frame has to, or
     * the avatar lands outside the content column entirely. `(852 − 2 × 64)` puts the column edge at 64dp.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `the agent avatar sits on the content column's leading edge`() {
        showConversation()

        // The frame merges its descendants' semantics, so the avatar is only addressable unmerged.
        val avatar = composeTestRule
            .onAllNodesWithTag("message_avatar", useUnmergedTree = true)[0]
            .getUnclippedBoundsInRoot()

        assertEquals(64f, avatar.left.value, TOLERANCE)
    }

    /**
     * A compact window draws it the other way round: the bubble on the column's edge and the avatar overhanging
     * into the screen margin, which is wide enough to hold the half-avatar. So the frame reserves no room here,
     * and the avatar lands a half-avatar outside the 16dp column.
     */
    @Test
    fun `the agent avatar overhangs a compact window's content column`() {
        showConversation()

        val avatar = composeTestRule
            .onAllNodesWithTag("message_avatar", useUnmergedTree = true)[0]
            .getUnclippedBoundsInRoot()

        assertEquals(4f, avatar.left.value, TOLERANCE)
    }

    /**
     * Width of the first audio cell in the conversation.
     *
     * Both cells are capped identically — the share button sits outside the bubble rather than being paid for
     * out of it, and the avatar's own room comes off the frame, not the cap — so measuring one covers both
     * directions, and the screenshots cover the two alignments.
     */
    private fun audioCellWidth(): Float = audioNode("audio_attachment").width.value

    /** Bounds of the first node carrying [tag], the conversation holding one cell per direction. */
    private fun audioNode(tag: String): DpRect =
        composeTestRule.onAllNodesWithTag(tag)[0].getUnclippedBoundsInRoot()

    private fun centreX(tag: String): Float = audioNode(tag).let { it.left.value + it.width.value / 2 }

    private fun showConversation() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { AudioConversation() }
        }
        composeTestRule.awaitAudioCell()
    }

    @Composable
    private fun AudioConversation() {
        ChatTheme {
            ChatConversation(
                conversationState = previewUiState(
                    messages = listOf(
                        PreviewAttachments.audioSdkMessage(ToClient),
                        PreviewAttachments.audioSdkMessage(ToAgent),
                    ),
                    isArchived = false,
                    isLiveChat = false,
                    // The typing indicator is on by default and would take part in the layout.
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

    private companion object {
        const val TOLERANCE = 0.5f

        /** Three touch targets and the gaps between them, plus the cell's own horizontal padding. */
        const val PLAYBACK_ROW_CELL_WIDTH = 216.0f

        /**
         * Material 3 pads a progress indicator's semantics bounds by 10dp above and below, so that a bar thinner
         * than the touch-target floor is still reachable. The bar's own height is what is left after that.
         */
        const val PROGRESS_SEMANTICS_INFLATION = 20f

        // The download manager is a process-wide singleton, and the conversation asks it for every audio
        // attachment whether or not a player is built. Reset per class rather than per test: nulling the
        // reference makes the next render build another manager, each with its own handler thread over the
        // one cache that nothing resets.
        @JvmStatic
        @AfterClass
        fun resetDownloadManager() = runBlocking { DownloadUtil.resetForTesting() }
    }
}

/**
 * Waits for the audio cells to compose, so nothing is measured against an empty conversation.
 *
 * `waitForIdle()` is enough for a conversation of text, once the mapping runs on a dispatcher the rule drives,
 * but not for this one: the cell also waits on the download manager's own thread, which no dispatcher here
 * reaches, so idle can arrive before the cell exists. Keyed to a tag rather than to text, since these fixtures
 * carry none.
 *
 * The bound is insurance rather than a measurement; reaching it means the cells never composed, so read the
 * fixture rather than raise it.
 */
private fun ComposeContentTestRule.awaitAudioCell() {
    waitUntil(timeoutMillis = SETTLE_TIMEOUT_MS) {
        onAllNodesWithTag("play_pause_button").fetchSemanticsNodes().isNotEmpty()
    }
}

private const val SETTLE_TIMEOUT_MS = 60_000L
