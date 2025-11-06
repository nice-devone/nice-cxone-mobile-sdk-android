/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Instrumentation tests for SelectAttachmentsView.
 * Verifies that attachments are displayed and callbacks are triggered as expected.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SelectAttachmentsViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestAttachments(count: Int): List<Attachment> =
        (1..count).map {
            object : Attachment {
                override val url = "https://example.com/file$it.jpg"
                override val mimeType = "image/jpeg"
                override val friendlyName = "Attachment $it"
            }
        }

    @Test
    fun attachmentsAreDisplayed() {
        val attachments = createTestAttachments(4)
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = attachments,
                    onAttachmentTapped = {},
                    onShare = {},
                    onCancel = {},
                )
            }
        }
        // Verify each attachment preview is displayed
        attachments.forEachIndexed { i, attachment ->
            composeTestRule.onNodeWithTag("attachment_preview_$i")
                .assertIsDisplayed()

            composeTestRule.onNodeWithText(attachment.friendlyName).let {
                // Usually only first two are visible, others might be off-screen
                if (i >= 2) it.assertExists() else it.assertIsDisplayed()
            }
        }
    }

    @Test
    fun onShareCalledOnlyWithNonEmptySelection() {
        val attachments = createTestAttachments(4)
        val shareCalled = AtomicBoolean(false)
        val sharedAttachments = AtomicReference<Collection<Attachment>>()
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = attachments,
                    onAttachmentTapped = {},
                    onShare = {
                        shareCalled.set(true)
                        sharedAttachments.set(it)
                    },
                    onCancel = {},
                )
            }
        }
        // Enter selection mode by long click on first attachment
        composeTestRule.onNodeWithTag("attachment_preview_0")
            .performTouchInput { longClick() }
        // Select second attachment
        composeTestRule.onNodeWithTag("attachment_preview_1")
            .performClick()
        // Click share button
        composeTestRule.onNodeWithTag("share_selected_button")
            .performClick()
        // Verify callback called and collection is not empty
        composeTestRule.runOnIdle {
            assert(shareCalled.get())
            val shared = sharedAttachments.get()
            assert(shared != null && shared.isNotEmpty())
        }
    }

    @Test
    fun onShareNotCalledWithEmptySelection() {
        val attachments = createTestAttachments(2)
        val shareCalled = AtomicBoolean(false)
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = attachments,
                    onAttachmentTapped = {},
                    onShare = { shareCalled.set(true) },
                    onCancel = {},
                )
            }
        }
        // Enter selection mode
        composeTestRule.onNodeWithTag("attachment_preview_0")
            .performTouchInput { longClick() }
        // Deselect all (assume testTag: "select_none_button")
        composeTestRule.onNodeWithTag("select_none_button")
            .performClick()
        // Try to share
        composeTestRule.onNodeWithTag("share_selected_button")
            .performClick()
        // Verify callback not called
        composeTestRule.runOnIdle {
            assert(!shareCalled.get())
        }
    }
}

