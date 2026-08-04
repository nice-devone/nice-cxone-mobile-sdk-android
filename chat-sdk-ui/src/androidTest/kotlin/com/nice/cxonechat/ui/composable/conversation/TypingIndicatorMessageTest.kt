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

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Person
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TypingIndicatorMessage composable.
 * Tests verify display, accessibility features, and semantic properties.
 */
@RunWith(AndroidJUnit4::class)
class TypingIndicatorMessageTest : AbstractComponentActivityUiTest() {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val expectedAccessibilityText = context.getString(R.string.text_agent_typing)
    private val agent = Person(id = "agent1", firstName = "Support", lastName = "Agent")

    @Test
    fun typingIndicatorMessage_isDisplayed() {
        setTypingIndicator()

        composeTestRule.onNodeWithTag("TypingIndicator").assertIsDisplayed()
    }

    @Test
    fun typingIndicatorMessage_hasContentDescription_forAccessibility() {
        setTypingIndicator()

        composeTestRule.onNode(
            hasTestTag("TypingIndicator") and hasContentDescription(expectedAccessibilityText)
        ).assertIsDisplayed()
    }

    @Test
    fun typingIndicatorMessage_hasAssertiveLiveRegionSemantics() {
        setTypingIndicator()

        composeTestRule.onNode(
            hasTestTag("TypingIndicator") and hasAssertiveLiveRegionSemantics()
        ).assertExists()
    }

    @Test
    fun typingIndicatorMessage_hasPopupSemantics() {
        setTypingIndicator()

        composeTestRule.onNode(
            hasTestTag("TypingIndicator") and hasPopupSemantics()
        ).assertExists()
    }

    private fun setTypingIndicator() {
        composeTestRule.setContent {
            ChatTheme {
                TypingIndicatorMessage(agent = agent)
            }
        }
    }

    private fun hasContentDescription(description: String) =
        SemanticsMatcher("Has content description: $description") { node ->
            val content = node.config.getOrNull(SemanticsProperties.ContentDescription)
            content == listOf(description)
        }

    private fun hasAssertiveLiveRegionSemantics() =
        SemanticsMatcher("Has assertive live region semantics") { node ->
            node.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Assertive
        }

    private fun hasPopupSemantics() =
        SemanticsMatcher("Has popup semantics") { node ->
            node.config.getOrNull(SemanticsProperties.IsPopup) != null
        }
}
