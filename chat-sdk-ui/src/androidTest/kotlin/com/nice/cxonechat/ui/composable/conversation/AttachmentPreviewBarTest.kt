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
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [AttachmentPreviewBar].
 *
 * Tests verify that the attachment preview bar above the input field provides proper
 * accessibility support including:
 * - Content descriptions for cancel/remove buttons
 * - Clickable actions with proper labels
 * - File name visibility and accessibility
 * - Focus order and navigation
 */
@RunWith(AndroidJUnit4::class)
class AttachmentPreviewBarTest : AbstractComponentActivityUiTest() {

    /**
     * Test that attachment preview bar is visible when attachments are present.
     */
    @Test
    fun attachmentPreviewBar_isDisplayed_whenAttachmentsPresent() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(PreviewAttachments.pdf),
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_bar")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that attachment file name is displayed and accessible.
     */
    @Test
    fun attachmentPreviewBar_displaysFileName() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(PreviewAttachments.pdf),
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        composeTestRule.onNodeWithText(PreviewAttachments.pdf.friendlyName)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that cancel/remove button has proper content description for accessibility.
     */
    @Test
    fun attachmentPreviewBar_cancelButton_hasContentDescription() {
        val attachment = PreviewAttachments.pdf
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(attachment),
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        // Verify cancel button has content description with file name
        composeTestRule
            .onNodeWithTag("preview_item_cancel_${attachment.friendlyName}", true)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .assertContentDescriptionEquals(
                getString(
                    resId = string.content_description_remove_prepared_attachment,
                    getString(
                        resId = string.content_description_attachment_single,
                        "${attachment.mimeTypeToDescription()} ${attachment.friendlyName}"
                    )
                )
            )
    }

    /**
     * Test that cancel/remove button is clickable and has click action.
     */
    @Test
    fun attachmentPreviewBar_cancelButton_isClickable() {
        var removedAttachment: Attachment? = null

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(PreviewAttachments.pdf),
                    onAttachmentClick = {},
                    onAttachmentRemoved = { removedAttachment = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("preview_item_cancel_${PreviewAttachments.pdf.friendlyName}")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assert(removedAttachment == PreviewAttachments.pdf) { "Remove callback should be invoked" }
    }

    /**
     * Test that attachment item itself is clickable.
     */
    @Test
    fun attachmentPreviewBar_attachmentItem_isClickable() {
        var clickedAttachment: Attachment? = null

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(PreviewAttachments.pdf),
                    onAttachmentClick = { clickedAttachment = it },
                    onAttachmentRemoved = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("preview_item_${PreviewAttachments.pdf.friendlyName}")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assert(clickedAttachment == PreviewAttachments.pdf) { "Click callback should be invoked" }
    }

    /**
     * Test multiple attachments are displayed with correct test tags.
     */
    @Test
    fun attachmentPreviewBar_multipleAttachments_allDisplayed() {
        val attachment1 = PreviewAttachments.pdf
        val attachment2 = PreviewAttachments.image

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(attachment1, attachment2),
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        // Verify both items are present with test tags
        composeTestRule.onNodeWithTag("attachment_preview_item_0")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("attachment_preview_item_1")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify file names
        composeTestRule.onNodeWithText(attachment1.friendlyName)
            .assertExists()
            .tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText(attachment2.friendlyName)
            .assertExists()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that each attachment has its own cancel button.
     */
    @Test
    fun attachmentPreviewBar_multipleAttachments_eachHasCancelButton() {
        val attachment1 = PreviewAttachments.pdf
        val attachment2 = PreviewAttachments.image
        val attachments = listOf(attachment1, attachment2)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = attachments,
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        // Verify each has cancel button with proper content description
        attachments.forEachIndexed { index, attachment ->
            composeTestRule
                .onNodeWithTag("preview_item_cancel_${attachment.friendlyName}", true)
                .assertExists()
                .tryPerformAccessibilityChecks()
                .assertContentDescriptionEquals(
                    getString(
                        resId = string.content_description_remove_prepared_attachment,
                        getString(
                            resId = string.content_description_attachment_item,
                            "${attachment.mimeTypeToDescription()} ${attachment.friendlyName}",
                            index + 1,
                            attachments.size
                        )
                    ),
                )
        }
    }

    /**
     * Test that attachment item has the correct accessibility click label (Gap 1).
     */
    @Test
    fun attachmentPreviewBar_attachmentItem_hasClickLabel() {
        val attachment = PreviewAttachments.pdf

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = listOf(attachment),
                    onAttachmentClick = {},
                    onAttachmentRemoved = {}
                )
            }
        }

        val expectedLabel = getString(
            string.content_description_prepared_attachment,
            getString(string.content_description_attachment_single, "${attachment.mimeTypeToDescription()} ${attachment.friendlyName}")
        )
        composeTestRule.onNodeWithTag("preview_item_${attachment.friendlyName}")
            .assert(
                SemanticsMatcher("has onClickLabel '$expectedLabel'") { node ->
                    runCatching { node.config[SemanticsActions.OnClick] }.getOrNull()?.label == expectedLabel
                }
            )
            .assertHasClickAction()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that removing one attachment keeps others visible.
     */
    @Test
    fun attachmentPreviewBar_removingOneAttachment_keepsOthers() {
        val attachment1 = PreviewAttachments.pdf
        val attachment2 = PreviewAttachments.image

        val attachments = mutableListOf(attachment1, attachment2)

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewBar(
                    attachments = attachments,
                    onAttachmentClick = {},
                    onAttachmentRemoved = { attachments.remove(it) }
                )
            }
        }

        // Remove first attachment
        composeTestRule.onNodeWithTag("preview_item_cancel_${attachment1.friendlyName}")
            .performClick()

        composeTestRule.waitForIdle()

        // Second attachment should still be visible
        composeTestRule.onNodeWithText(attachment2.friendlyName)
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }
}
