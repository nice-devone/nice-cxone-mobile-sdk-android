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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Accessibility tests for [AttachmentPreviewGroup].
 *
 * Tests verify that the attachment preview grid provides proper
 * TalkBack navigation semantics, including:
 * - Traversal group marking
 * - Content descriptions for each attachment item that include the attachment type and position
 */
@RunWith(AndroidJUnit4::class)
class AttachmentPreviewGroupAccessibilityTest : AbstractComponentActivityUiTest() {

    private fun createMockAttachment(index: Int): Attachment = object : Attachment {
        override val url = "https://example.com/attachment_$index.jpg"
        override val friendlyName = "attachment_$index.jpg"
        override val mimeType = "image/jpeg"
    }

    private fun createAttachments(count: Int) = (1..count).map { createMockAttachment(it) }

    @Test
    fun attachmentPreviewGroup_isMarkedAsTraversalGroup() {
        val attachments = createAttachments(2)
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewGroup(
                    messageId = UUID.randomUUID(),
                    attachments = attachments,
                    onAttachmentClicked = {},
                    onMoreClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_group")
            .assert(
                SemanticsMatcher("is traversal group") { node ->
                    node.config.getOrNull(SemanticsProperties.IsTraversalGroup) == true
                }
            )
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun attachmentPreviewGroup_firstItem_hasContentDescription() {
        val attachments = createAttachments(3)
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewGroup(
                    messageId = UUID.randomUUID(),
                    attachments = attachments,
                    onAttachmentClicked = {},
                    onMoreClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_0")
            .assertContentDescriptionEquals(
                getString(
                    string.content_description_attachment_item,
                    "${attachments[0].mimeTypeToDescription()} ${attachments[0].friendlyName}",
                    1,
                    3
                )
            )
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun attachmentPreviewGroup_secondItem_hasContentDescription() {
        val attachments = createAttachments(3)
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewGroup(
                    messageId = UUID.randomUUID(),
                    attachments = attachments,
                    onAttachmentClicked = {},
                    onMoreClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_1")
            .assertContentDescriptionEquals(
                getString(
                    string.content_description_attachment_item,
                    "${attachments[1].mimeTypeToDescription()} ${attachments[1].friendlyName}",
                    2,
                    3
                )
            )
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun attachmentPreviewGroup_thirdItem_hasContentDescription() {
        val attachments = createAttachments(3)
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPreviewGroup(
                    messageId = UUID.randomUUID(),
                    attachments = attachments,
                    onAttachmentClicked = {},
                    onMoreClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_2")
            .assertContentDescriptionEquals(
                getString(
                    string.content_description_attachment_item,
                    "${attachments[2].mimeTypeToDescription()} ${attachments[2].friendlyName}",
                    3,
                    3
                )
            )
            .tryPerformAccessibilityChecks()
    }
}
