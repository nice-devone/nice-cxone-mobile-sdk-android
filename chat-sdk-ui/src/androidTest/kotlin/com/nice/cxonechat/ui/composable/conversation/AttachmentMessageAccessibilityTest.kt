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

import android.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import coil3.test.FakeImageLoaderEngine.Builder
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.AudioAttachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.Text
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for [AttachmentMessage].
 *
 * Tests verify that attachments displayed in conversation messages provide proper
 * accessibility support including:
 * - Clickable attachment previews
 * - Long-press actions for "more attachments"
 * - Proper test tags for automation
 * - Share functionality accessibility
 * - Overflow indicators for multiple attachments
 */
@RunWith(AndroidJUnit4::class)
@Suppress(
    "LargeClass" // Complex test
)
class AttachmentMessageAccessibilityTest : AbstractComponentActivityUiTest() {

    private fun createMockAttachment(suffix: String = "", name: String = "file$suffix.pdf"): Attachment = object : Attachment {
        override val url = "https://example.com/$name"
        override val friendlyName = name
        override val mimeType = "application/pdf"
    }

    private fun createAttachmentMessage(attachments: List<Attachment>) = WithAttachments(
        message = Text(),
        attachments = attachments
    )

    /**
     * Test single attachment is displayed and clickable.
     */
    @Test
    fun attachmentMessage_singleAttachment_hasContentDescription() {
        val attachment = PreviewAttachments.pdf
        val message = createAttachmentMessage(listOf(attachment))
        var description: String? = null
        composeTestRule.setContent {
            description = attachment.mimeTypeToDescription()
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }
        requireNotNull(description)

        composeTestRule.onNodeWithTag("attachment_message")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("attachment_preview_${attachment.url}")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
            .tryPerformAccessibilityChecks()
            .assertContentDescriptionEquals(description)
            .performClick()
    }

    /**
     * Test overflow indicator appears when too many attachments.
     */
    @Test
    fun attachmentMessage_manyAttachments_showsOverflowIndicator() {
        // Create more attachments than can fit (typically 4 max)
        val attachments = (1..5).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        // Overflow indicator should be visible
        composeTestRule.onNodeWithTag("attachment_overflow")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Should show "+1" for the extra attachment
        composeTestRule.onNodeWithText("+1")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test more attachments callback is invoked.
     */
    @Test
    fun attachmentMessage_manyAttachments_moreClickWorks() {
        val attachments = (1..5).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)
        var moreClickedWith: List<Attachment>? = null

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = { moreClickedWith = it },
                    onShare = {}
                )
            }
        }

        // Click on the blurred/overflow attachment
        composeTestRule.onNodeWithTag("attachment_preview_3", true)
            .assertIsDisplayed()
            .performClick()

        assertNotNull(moreClickedWith)
        assert(moreClickedWith == attachments) { "More clicked should pass all attachments" }
    }

    /**
     * Test attachment message has proper test tag.
     */
    @Test
    fun attachmentMessage_hasProperTestTag() {
        val attachment = createMockAttachment(name = "test.pdf")
        val message = createAttachmentMessage(listOf(attachment))

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_message")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test audio attachment is displayed correctly.
     */
    @Test
    @Ignore("Audio player component requires refactor to be testable")
    fun attachmentMessage_audioAttachment_isDisplayed() {
        val message = AudioAttachment(
            message = Text(),
            attachment = createMockAttachment(name = "audio.mp3")
        )

        composeTestRule.setContent {
            ChatTheme {
                AudioAttachment(
                    attachment = message
                )
            }
        }

        // Audio attachments should use audio player
        composeTestRule.onNodeWithTag("audio_player")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test missing attachments displays error message.
     */
    @Test
    fun attachmentMessage_noAttachments_displaysError() {
        val message = createAttachmentMessage(emptyList())

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("missing_attachments", true)
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test grouped attachment items have proper click labels for accessibility (Gap 2).
     */
    @Test
    fun attachmentMessage_multipleAttachments_itemsHaveClickLabels() {
        val attachments = (1..3).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        val expectedLabel = getString(string.content_description_attachment_detail)
        composeTestRule.onNodeWithTag("attachment_preview_0")
            .assertExists()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher("has onClickLabel '$expectedLabel'") { node ->
                    runCatching { node.config[SemanticsActions.OnClick] }.getOrNull()?.label == expectedLabel
                }
            )
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test grouped attachment items have proper long-click labels for accessibility (Gap 2).
     */
    @Test
    fun attachmentMessage_multipleAttachments_itemsHaveLongClickLabels() {
        val attachments = (1..3).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        val expectedLabel = getString(string.content_description_attachments_sharing)
        composeTestRule.onNodeWithTag("attachment_preview_0")
            .assertExists()
            .assert(
                SemanticsMatcher("has onLongClickLabel '$expectedLabel'") { node ->
                    runCatching { node.config[SemanticsActions.OnLongClick] }.getOrNull()?.label == expectedLabel
                }
            )
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the overflow attachment item has the "show all" click label (Gap 3).
     */
    @Test
    fun attachmentMessage_manyAttachments_overflowItemHasShowAllLabel() {
        val attachments = (1..5).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        val expectedLabel = getString(string.content_description_show_all_attachments, attachments.size)
        // The blurred overflow item is attachment_preview_3 (4th item, index in second row = 1)
        composeTestRule.onNodeWithTag("attachment_preview_3")
            .assertExists()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher("has onClickLabel '$expectedLabel'") { node ->
                    runCatching { node.config[SemanticsActions.OnClick] }.getOrNull()?.label == expectedLabel
                }
            )
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that long-pressing a single image attachment announces "Share" to TalkBack (Gap 2).
     *
     * Uses [FakeImageLoaderEngine] to intercept Coil requests so no network access is needed.
     */
    @OptIn(DelicateCoilApi::class)
    @Test
    fun singleAttachment_longClickLabel_usesShareLabel() {
        val engine = Builder()
            .default(ColorImage(Color.CYAN))
            .build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(composeTestRule.activity)
                .components { add(engine) }
                .build()
        )
        val attachment = object : Attachment {
            override val url = "file:///test_image.jpg"
            override val friendlyName = "test_image.jpg"
            override val mimeType = "image/jpeg"
        }
        val message = createAttachmentMessage(listOf(attachment))

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        val expectedLabel = getString(string.share_attachment_selected)
        composeTestRule.onNodeWithTag("attachment_preview_${attachment.url}")
            .assertExists()
            .assert(
                SemanticsMatcher("has onLongClickLabel '$expectedLabel'") { node ->
                    runCatching { node.config[SemanticsActions.OnLongClick] }.getOrNull()?.label == expectedLabel
                }
            )
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the root node of an attachment message has a content description announcing the attachment count.
     */
    @Test
    fun attachmentMessage_rootNode_hasContentDescriptionWithAttachmentCount() {
        val attachments = (1..3).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)
        var accessibilityDescription: String? = null

        composeTestRule.setContent {
            accessibilityDescription = message.getMessageAccessibility(
                overrideText = stringResource(string.content_description_attachment_message, attachments.size)
            )
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }
        requireNotNull(accessibilityDescription)

        composeTestRule.onNodeWithTag("attachment_message_description", useUnmergedTree = true)
            .assertContentDescriptionEquals(accessibilityDescription)
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the root node of a single-attachment message has a content description.
     */
    @Test
    fun attachmentMessage_singleAttachment_rootNodeHasContentDescription() {
        val attachment = createMockAttachment(name = "file1.pdf")
        val message = createAttachmentMessage(listOf(attachment))
        var accessibilityDescription: String? = null

        composeTestRule.setContent {
            accessibilityDescription = message.getMessageAccessibility(
                overrideText = stringResource(string.content_description_attachment_single, attachment.mimeTypeToDescription())
            )
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }
        requireNotNull(accessibilityDescription)

        composeTestRule.onNodeWithTag("attachment_message_description", useUnmergedTree = true)
            .assertContentDescriptionEquals(accessibilityDescription)
            .tryPerformAccessibilityChecks()
    }

    /**
     * Verifies getMessageAccessibility uses "AI Assistant" as the sender name for AI-generated messages.
     *
     * This guards EU AI Act Article 50 compliance: TalkBack must announce AI authorship, not the
     * human agent's name, when a message carries isAiGenerated = true.
     */
    @Test
    fun getMessageAccessibility_aiGeneratedMessage_usesAiAssistantLabel() {
        val aiMessage = Message.Text(Text(isAiGenerated = true))
        var accessibilityText: String? = null

        composeTestRule.setContent {
            accessibilityText = aiMessage.getMessageAccessibility()
        }

        val aiLabel = getString(string.ai_assistant_label)
        assertNotNull(accessibilityText)
        assertTrue(
            "Expected accessibility text to contain '$aiLabel' but was: $accessibilityText",
            accessibilityText!!.contains(aiLabel),
        )
    }

    /**
     * Verifies getMessageAccessibility does NOT use the AI Assistant label for non-AI messages.
     */
    @Test
    fun getMessageAccessibility_nonAiMessage_doesNotUseAiAssistantLabel() {
        val humanMessage = Message.Text(Text(isAiGenerated = false))
        var accessibilityText: String? = null

        composeTestRule.setContent {
            accessibilityText = humanMessage.getMessageAccessibility()
        }

        val aiLabel = getString(string.ai_assistant_label)
        assertNotNull(accessibilityText)
        assertFalse(
            "Expected accessibility text not to contain AI label for non-AI message, but was: $accessibilityText",
            accessibilityText!!.contains(aiLabel),
        )
    }

    /**
     * Test all attachments in a group are accessible.
     */
    @Test
    fun attachmentMessage_allAttachmentsHaveTestTags() {
        val attachments = (1..2).map { createMockAttachment("$it") }
        val message = createAttachmentMessage(attachments)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentMessage(
                    message = message,
                    modifier = Modifier,
                    onShowFrame = {},
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {}
                )
            }
        }

        // Both attachments should have test tags
        attachments.forEachIndexed { index, _ ->
            composeTestRule.onNodeWithTag("attachment_preview_$index")
                .assertExists()
                .tryPerformAccessibilityChecks()
        }
    }
}
