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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SelectAttachmentsViewAccessibilityTest : AbstractComponentActivityUiTest() {

    @Test
    fun testSelectionModeToggleAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = PreviewAttachments.choices,
                    onAttachmentTapped = {},
                    onShare = {},
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("select_attachments_false")
            .assertExists()
            .assertContentDescriptionEquals(getString(string.content_description_select_button))
            .performClick()

        composeTestRule.onNodeWithTag("share_selected_button")
            .assertExists()
            .assertContentDescriptionEquals(getString(string.share_attachment_selected))
            .performClick()
    }

    @Test
    fun testTitleAndHeaderAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = listOf(),
                    onAttachmentTapped = {},
                    onShare = {},
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithText(getString(string.attachments_title))
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the attachments modal exposes a TalkBack-accessible dismiss action.
     */
    @Test
    fun selectAttachmentsView_hasDismissAction_callsOnCancel() {
        var cancelled = false

        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = emptyList(),
                    onAttachmentTapped = {},
                    onShare = {},
                    onCancel = { cancelled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("select_attachments_view")
            .assertExists()
            .assert(
                SemanticsMatcher("has Dismiss action") { node ->
                    runCatching { node.config[SemanticsActions.Dismiss] }.getOrNull() != null
                }
            )
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }

        assertTrue("onCancel should be called when dismiss action is invoked", cancelled)
    }

    @Test
    fun testTapToOpenSelectActions() {
        composeTestRule.setContent {
            ChatTheme {
                SelectAttachmentsView(
                    attachments = PreviewAttachments.choices,
                    onAttachmentTapped = {},
                    onShare = {},
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_preview_0")
            .assertExists()
            .performClick()
    }
}
