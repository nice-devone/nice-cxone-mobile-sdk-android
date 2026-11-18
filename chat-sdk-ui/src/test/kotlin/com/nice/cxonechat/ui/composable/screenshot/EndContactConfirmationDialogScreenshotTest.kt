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
import com.nice.cxonechat.ui.composable.conversation.EndContactConfirmationDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens for the end conversation confirmation dialog (DE-163624): unlike the end conversation and
 * inactivity sheets, no design exists for a wide-screen cap on this dialog, so it is deliberately left
 * out of scope here. [EndContactConfirmationDialog] builds on [ChatTheme.Dialog], which draws its
 * [androidx.compose.ui.window.DialogProperties] with `usePlatformDefaultWidth = false`, so it fills the
 * window edge to edge on every window size rather than staying a centred card in landscape; these goldens
 * pin that unchanged behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EndContactConfirmationDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `end conversation confirmation dialog portrait`() {
        captureDialog()
    }

    @Test
    @Config(qualifiers = "w412dp-h852dp-normal-notlong-notround-night-xhdpi-finger")
    fun `end conversation confirmation dialog portrait dark`() {
        captureDialog(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `end conversation confirmation dialog landscape`() {
        captureDialog()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `end conversation confirmation dialog landscape dark`() {
        captureDialog(darkTheme = true)
    }

    private fun captureDialog(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The dialog is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the scrim either side of it to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    EndContactConfirmationDialog(
                        onConfirm = {},
                        onCancel = {},
                    )
                }
            }
        }
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(DIALOG_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val DIALOG_TAG = "end_conversation_confirmation_dialog"
    }
}
