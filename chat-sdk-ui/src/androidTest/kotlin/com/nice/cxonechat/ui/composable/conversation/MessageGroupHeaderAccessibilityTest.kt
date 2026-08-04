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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for [MessageGroupHeader].
 *
 * Tests verify that the date header provides proper TalkBack semantics:
 * - Merged semantics node with contentDescription
 * - Optional traversalIndex for reading order
 * - Child text hidden from accessibility (avoids double-reading)
 */
@RunWith(AndroidJUnit4::class)
class MessageGroupHeaderAccessibilityTest : AbstractComponentActivityUiTest() {

    @Test
    fun messageGroupHeader_mergedSemantics_announcesDateAndHidesChildText() {
        val dayString = "Today"
        composeTestRule.setContent {
            ChatTheme {
                MessageGroupHeader(dayString = dayString)
            }
        }

        // Parent announces the date via contentDescription (merged semantics node)
        composeTestRule.onNodeWithTag("message_group_header_row")
            .assertContentDescriptionEquals(dayString)
            .tryPerformAccessibilityChecks()

        // Child Text is hidden from accessibility to avoid double-reading
        composeTestRule.onNodeWithText(dayString).assertDoesNotExist()
    }

    @Test
    fun messageGroupHeader_withTraversalIndex_setsTraversalIndex() {
        val dayString = "Yesterday"
        val traversalIndex = 100f
        composeTestRule.setContent {
            ChatTheme {
                MessageGroupHeader(
                    dayString = dayString,
                    traversalIndexBase = traversalIndex
                )
            }
        }

        composeTestRule.onNodeWithTag("message_group_header_row")
            .assert(
                SemanticsMatcher("has traversal index $traversalIndex") { node ->
                    node.config.getOrNull(SemanticsProperties.TraversalIndex) == traversalIndex
                }
            )
    }
}
