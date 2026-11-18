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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [ChatTheme.SheetTopBar], the header of a modally presented sheet.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class SheetTopBarTest : AbstractComponentActivityUiTest() {

    private val title = "Select a time"
    private val subtitle = "Please select a preferred time slot."

    @Test
    fun sheetTopBar_showsTitleAndSubtitle() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.SheetTopBar(title = title, subtitle = subtitle)
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitle).assertIsDisplayed()
    }

    @Test
    fun sheetTopBar_omitsSubtitle_whenNotSupplied() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.SheetTopBar(title = title)
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(subtitle).assertDoesNotExist()
    }

    /**
     * The migrated sheet headers pass their existing test tags through these modifiers, so the tags
     * have to reach the rendered title and subtitle.
     */
    @Test
    fun sheetTopBar_passesTestTagsThroughToTitleAndSubtitle() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.SheetTopBar(
                    title = title,
                    subtitle = subtitle,
                    titleModifier = Modifier.testTag("sheet_title"),
                    subtitleModifier = Modifier.testTag("sheet_subtitle"),
                )
            }
        }

        composeTestRule.onNodeWithTag("sheet_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sheet_subtitle").assertIsDisplayed()
    }

    /**
     * The sheets keep their existing dismissal affordances, so the bar must not introduce a
     * navigation icon of its own.
     */
    @Test
    fun sheetTopBar_hasNoNavigationIcon_byDefault() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.SheetTopBar(title = title, subtitle = subtitle)
            }
        }

        composeTestRule.onNodeWithTag("back_button").assertDoesNotExist()
    }

    @Test
    fun sheetTopBar_showsTrailingAction_whenSupplied() {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.SheetTopBar(
                    title = title,
                    actions = { Text("Select", modifier = Modifier.testTag("sheet_action")) },
                )
            }
        }

        composeTestRule.onNodeWithTag("sheet_action").assertIsDisplayed()
    }
}
