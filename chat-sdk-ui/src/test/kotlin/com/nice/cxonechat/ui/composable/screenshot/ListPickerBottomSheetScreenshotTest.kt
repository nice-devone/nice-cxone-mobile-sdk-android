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

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.ListPickerBottomSheet
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker
import com.nice.cxonechat.ui.util.preview.message.manyOptionsListPicker
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

private const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"
private const val OVERFLOW_OPTION_COUNT = 20

/**
 * Goldens for the list picker sheet, which shares [com.nice.cxonechat.ui.composable.theme.ModalBottomSheet]
 * with the pre-chat survey and inactivity sheets: the title and options keep the design's 16dp portrait
 * margin and widen to the shared 64dp inset in a landscape window. The footer buttons are unaffected —
 * they keep the inset they already had, from Material 3's own default content padding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListPickerBottomSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The "Reply with a cat image" option loads its leading icon through Coil's AsyncImage from a live
    // URL, which has no network under Robolectric and otherwise races the capture: whether the fallback
    // placeholder or the real image wins depends on the runner, not the code under test. A fake engine
    // that resolves synchronously removes the race, matching RichLinkConversationScreenshotTest.
    @OptIn(DelicateCoilApi::class, ExperimentalCoilApi::class)
    @Before
    fun installFakeImage() {
        val engine = FakeImageLoaderEngine(ColorImage(color = android.graphics.Color.CYAN, width = 64, height = 64))
        val imageLoader = ImageLoader.Builder(ApplicationProvider.getApplicationContext())
            .components { add(engine) }
            .coroutineContext(EmptyCoroutineContext)
            .mainCoroutineContext(Dispatchers.Main.immediate)
            .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `list picker bottom sheet portrait`() {
        setContent()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `list picker bottom sheet portrait keeps the design's equal 16dp side margins`() {
        setContent()

        composeTestRule.assertSideMargins(
            container = composeTestRule.onNodeWithTag("list_picker_bottom_sheet"),
            content = composeTestRule.onAllNodesWithTag("list_picker_item")[0],
            expected = 16.dp,
        )
    }

    @Test
    fun `list picker options follow the design's row layout`() {
        setContent()
        val rows = composeTestRule.onAllNodesWithTag("list_picker_item")
        val textOnlyRow = rows[0].fetchSemanticsNode().boundsInWindow
        val imageRow = rows[1].fetchSemanticsNode().boundsInWindow
        val textOnlyLabel = boundsOfText("Reply without media")
        val imageRowLabel = boundsOfText("Reply with a cat image")
        val imageRowDescription = boundsOfText("Some long description to go with the image")
        val imageRowCheck = composeTestRule.onAllNodesWithTag("list_picker_item_check", useUnmergedTree = true)[1]
            .fetchSemanticsNode()
            .boundsInWindow

        with(composeTestRule.density) {
            assertEquals("content inset", 12.dp.toPx(), textOnlyLabel.left - textOnlyRow.left, 0.5f)
            assertEquals("text after the image", (12 + 64 + 12).dp.toPx(), imageRowLabel.left - imageRow.left, 0.5f)
            assertEquals("supporting text under the label", imageRowLabel.bottom, imageRowDescription.top, 0.5f)
            assertEquals("check size", 24.dp.toPx(), imageRowCheck.width, 0.5f)
            assertEquals("check end inset", 12.dp.toPx(), imageRow.right - imageRowCheck.right, 0.5f)
            // The design draws a text-only row 40dp tall; it keeps the 48dp minimum touch target instead.
            assertEquals("text-only row height", 48.dp.toPx(), textOnlyRow.height, 0.5f)
            assertEquals("row with an image", 80.dp.toPx(), imageRow.height, 0.5f)
        }
    }

    @Test
    fun `list picker bottom sheet portrait with a selected option`() {
        setContent()
        val option = composeTestRule.onAllNodesWithTag("list_picker_item")[1]
        option.performClick()
        option.assertIsSelected()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `list picker bottom sheet portrait dark`() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `list picker bottom sheet landscape`() {
        setContent()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `list picker bottom sheet landscape dark`() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `list picker bottom sheet portrait with overflowing options shows scrollbar`() {
        setContent(message = ListPicker(manyOptionsListPicker(OVERFLOW_OPTION_COUNT)) {})
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private fun boundsOfText(text: String) = composeTestRule.onNodeWithText(text, useUnmergedTree = true)
        .fetchSemanticsNode()
        .boundsInWindow

    private fun setContent(darkTheme: Boolean = false, message: ListPicker = ListPicker(UiSdkListPicker()) {}) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                ListPickerBottomSheet(
                    message = message,
                    onDismiss = {},
                    onDone = {}
                )
            }
        }
    }
}
