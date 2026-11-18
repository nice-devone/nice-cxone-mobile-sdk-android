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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.generic.AttachmentPickerDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.source.AllowedFileType
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the drawn appearance of the attachment picker sheet: the spacing above and below its title, the
 * dividers between its options, and the absence of a drag handle.
 *
 * [AttachmentPickerDialog] builds its own sheet, so it is composed whole rather than through a stand-in
 * host. That is what lets these goldens pin the sheet's own chrome — a replica could show any handle and any
 * top padding the test chose to give it.
 *
 * Each window matches a design frame, so a golden's pixel offsets can be read against the frame's own
 * measurements directly: 412dp wide for the portrait frame, 852x412 for the landscape one. The landscape
 * goldens pin this sheet's departure from the module's width rule — it keeps Material 3's centred 640dp
 * cap, with its options inset inside that card, where
 * [com.nice.cxonechat.ui.composable.theme.wideScreenSheetMaxWidth] would have it span the window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentPickerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `attachment picker portrait`() {
        capturePicker()
    }

    @Test
    @Config(qualifiers = "w412dp-h852dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment picker portrait dark`() {
        capturePicker(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment picker landscape`() {
        capturePicker()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment picker landscape dark`() {
        capturePicker(darkTheme = true)
    }

    private fun capturePicker(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the sheet's scrim to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    AttachmentPickerDialog(
                        onDismiss = {},
                        getContent = {},
                        allowedFileTypeSource = ALLOWED_FILE_TYPES,
                    )
                }
            }
        }

        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(FIRST_OPTION_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        // The sheet slides in, and its content travels with it, so an unsettled frame would record the
        // options mid-travel.
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val FIRST_OPTION_TAG = "option_0"

        /**
         * Both media wildcards, which is what makes all four options allowed: a camera photo, a camera
         * video, the combined media picker, and a file browser. Fewer types would drop rows and with them
         * the dividers between them.
         */
        val ALLOWED_FILE_TYPES = object : AllowedFileTypeSource {
            override val allowedMimeTypes: List<AllowedFileType> = listOf(
                AllowedFileType(mimeType = "image/*", description = "Images"),
                AllowedFileType(mimeType = "video/*", description = "Videos"),
            )
        }
    }
}
