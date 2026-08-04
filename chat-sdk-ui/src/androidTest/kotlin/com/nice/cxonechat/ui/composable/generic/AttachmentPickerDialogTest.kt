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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.AttachmentType.CameraPhoto
import com.nice.cxonechat.ui.AttachmentType.CameraVideo
import com.nice.cxonechat.ui.AttachmentType.File
import com.nice.cxonechat.ui.AttachmentType.ImageAndVideo
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.source.AllowedFileType
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@LargeTest
class AttachmentPickerDialogTest : AbstractComponentActivityUiTest() {

    private val allowedFileTypes = listOf(
        AllowedFileType(
            mimeType = "image/*",
            description = "Image"
        ),
        AllowedFileType(
            mimeType = "video/*",
            description = "Video"
        ),
        AllowedFileType(
            mimeType = "application/pdf",
            description = "PDF"
        )
    )

    private val fakeAllowedFileTypeSource = object : AllowedFileTypeSource {
        override val allowedMimeTypes: List<AllowedFileType> = allowedFileTypes
    }

    @Test
    fun actionsAreDisplayed() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }
        // Check that all expected action labels are displayed
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(string.attachment_type_camera_image)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(string.attachment_type_camera_video)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(string.attachment_type_media)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(string.attachment_type_file)
        ).assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun getContentIsCalledWithCorrectAttachmentType() {
        val selectedType = AtomicReference<AttachmentType?>(null)
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = { selectedType.set(it) },
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }
        // Click on each option and verify callback
        val options = listOf(
            Pair("option_0", CameraPhoto),
            Pair("option_1", CameraVideo),
            Pair("option_2", ImageAndVideo),
            Pair("option_3", File(arrayOf("image/*", "video/*", "application/pdf")))
        )
        options.forEach { (testTag, expectedType) ->
            selectedType.set(null)
            composeTestRule.onNodeWithTag(testTag).performClick()
            composeTestRule.runOnIdle {
                assertEquals(expectedType, selectedType.get())
            }
        }
    }

    /**
     * Test that the bottom sheet is displayed with proper accessibility.
     */
    @Test
    fun attachmentPickerDialog_bottomSheet_hasAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }

        composeTestRule.onNodeWithTag("attachment_picker_bottom_sheet")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the title has proper content description.
     */
    @Test
    fun attachmentPickerDialog_title_hasContentDescription() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }

        // The title uses clearAndSetSemantics with testTag, so we use the test tag
        composeTestRule.onNodeWithTag("top_title")
            .assertExists()
            .tryPerformAccessibilityChecks()

        // Also verify the content description is set on the parent container
        val title = getString(string.title_attachment_picker)
        composeTestRule.onNodeWithContentDescription(title)
            .assertExists()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that each option button has proper accessibility.
     */
    @Test
    fun attachmentPickerDialog_optionButtons_haveAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }

        // Check each option is accessible
        listOf("option_0", "option_1", "option_2", "option_3").forEach { tag ->
            composeTestRule.onNodeWithTag(tag)
                .assertExists()
                .assertIsDisplayed()
                .tryPerformAccessibilityChecks()
        }
    }

    /**
     * Test that all option labels are accessible for screen readers.
     */
    @Test
    fun attachmentPickerDialog_optionLabels_areAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = fakeAllowedFileTypeSource
                )
            }
        }

        val labels = listOf(
            string.attachment_type_camera_image,
            string.attachment_type_camera_video,
            string.attachment_type_media,
            string.attachment_type_file
        )

        labels.forEach { labelRes ->
            val label = composeTestRule.activity.getString(labelRes)
            composeTestRule.onNodeWithText(label)
                .assertIsDisplayed()
                .tryPerformAccessibilityChecks()
        }
    }

    /**
     * Test that the dialog works properly with limited allowed file types.
     */
    @Test
    fun attachmentPickerDialog_withLimitedTypes_hasAccessibility() {
        val limitedFileTypes = listOf(
            AllowedFileType(
                mimeType = "image/*",
                description = "Image"
            )
        )
        val limitedSource = object : AllowedFileTypeSource {
            override val allowedMimeTypes: List<AllowedFileType> = limitedFileTypes
        }

        composeTestRule.setContent {
            ChatTheme {
                AttachmentPickerDialog(
                    onDismiss = {},
                    getContent = {},
                    allowedFileTypeSource = limitedSource
                )
            }
        }

        // Verify the bottom sheet is still accessible with limited options
        composeTestRule.onNodeWithTag("attachment_picker_bottom_sheet")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }
}
