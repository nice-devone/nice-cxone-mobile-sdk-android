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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.source.AllowedFileType
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@LargeTest
class AttachmentPickerDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
            composeTestRule.activity.getString(R.string.attachment_type_camera_image)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.attachment_type_camera_video)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.attachment_type_media)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.attachment_type_file)
        ).assertIsDisplayed()
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
            Pair("option_0", AttachmentType.CameraPhoto),
            Pair("option_1", AttachmentType.CameraVideo),
            Pair("option_2", AttachmentType.ImageAndVideo),
            Pair("option_3", AttachmentType.File(arrayOf("image/*", "video/*", "application/pdf")))
        )
        options.forEach { (testTag, expectedType) ->
            selectedType.set(null)
            composeTestRule.onNodeWithTag(testTag).performClick()
            composeTestRule.runOnIdle {
                assertEquals(expectedType, selectedType.get())
            }
        }
    }
}
