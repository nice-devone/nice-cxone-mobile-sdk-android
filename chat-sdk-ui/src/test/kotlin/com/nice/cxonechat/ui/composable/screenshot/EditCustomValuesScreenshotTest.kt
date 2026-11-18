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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.ui.composable.EditCustomValuesScreen
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.SheetAnchors
import com.nice.cxonechat.ui.domain.model.CustomValueItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

// Comfortably above Space.expandedWidthBreakpoint (840dp) while staying taller than wide, so
// isLandscapeWindow() is false but isWideScreen() is true. Mirrors ChatThemeTopBarTest's EXPANDED_WIDTH.
private const val PORTRAIT_EXPANDED_QUALIFIERS = "w1280dp-h1600dp-normal-notlong-notround-notnight-xhdpi-finger"

/**
 * Verifies the pre-chat survey / custom values sheet across the window configurations where its two
 * wide-screen predicates can diverge: a compact portrait window, a short landscape window where the
 * field list overflows and picks up the wide-screen inset, and a portrait window wide enough to count
 * as wide-screen (>= 840dp) but not landscape, which pins the sheet's max-width cap (see
 * [com.nice.cxonechat.ui.composable.theme.ModalBottomSheet]'s `sheetMaxWidth`, gated on
 * `isLandscapeWindow()` rather than `isWideScreen()` so this configuration keeps the 640dp Material 3
 * cap instead of going edge-to-edge).
 *
 * Robolectric does not dispatch real system-bar insets to the decor view (see
 * `OfflineScreenScreenshotTest`), so `WindowInsets.navigationBars` resolves to zero in every capture
 * here. The side-navigation-bar clearance on [com.nice.cxonechat.ui.composable.theme.ModalBottomSheet]
 * is therefore not exercised by these goldens; only the wide-screen padding and sheet width are.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditCustomValuesScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `edit custom values portrait shows cancel and submit buttons`() {
        setContent(portraitFields())

        // Regression: a weight() on the title/body column previously collapsed the footer to zero
        // height whenever the content did not force a bounded constraint, which portrait's shorter
        // field list does not.
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_button").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `edit custom values portrait keeps the design's equal 16dp side margins`() {
        setContent(portraitFields())

        composeTestRule.assertSideMargins(
            container = composeTestRule.onNodeWithTag("edit_custom_values_screen"),
            content = composeTestRule.onNodeWithTag("custom_value_text_Field 1"),
            expected = 16.dp,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `edit custom values portrait`() {
        setContent(portraitFields())
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `edit custom values landscape`() {
        setContent(landscapeFields())
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `edit custom values portrait dark`() {
        setContent(portraitFields(), darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `edit custom values landscape dark`() {
        setContent(landscapeFields(), darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = PORTRAIT_EXPANDED_QUALIFIERS)
    @Test
    fun `edit custom values portrait expanded keeps the sheet width capped`() {
        setContent(portraitFields())
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setContent(fields: List<CustomValueItem<*, *>>, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                val sheetState = remember {
                    SheetState(
                        enabledValues = SheetAnchors.FullHeightOnly,
                        initialValue = Expanded,
                        positionalThreshold = { 0f },
                        velocityThreshold = { 0f },
                    )
                }
                EditCustomValuesScreen(
                    title = "Pre-Contact Survey",
                    fields = fields,
                    sheetState = sheetState,
                    onCancel = {},
                    onConfirm = {},
                )
            }
        }
    }

    private fun portraitFields() = fields(count = 2)

    private fun landscapeFields() = fields(count = 7)

    private fun fields(count: Int) = (1..count).map { index ->
        CustomValueItem.Text(
            object : FieldDefinition.Text {
                override val fieldId = "field_$index"
                override val label = "Field $index"
                override val isEMail = false
                override val isRequired = false

                override fun validate(value: String) = Unit
            },
            null
        )
    }
}
