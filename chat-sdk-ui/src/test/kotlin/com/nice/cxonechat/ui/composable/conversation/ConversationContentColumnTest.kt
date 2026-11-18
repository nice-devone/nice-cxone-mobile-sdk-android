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

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.width
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.ChatConfigurationRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.nice.cxonechat.ui.util.preview.message.Text as MessageText

/**
 * Pins the inset between the window and the conversation's content column.
 *
 * The column belongs to the message list rather than to any one message, so plain text messages are used —
 * an assertion written against a particular message type would pass for reasons of its own. The wide inset
 * replaces the compact one rather than adding to it, which is the part that regressed: both applied at once
 * put the column 16dp further in than the design draws it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ConversationContentColumnTest {

    /** Maps the messages here rather than on a `Dispatchers.Default` thread a loaded runner may not schedule. */
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(dispatcher)

    @get:Rule
    val chatConfiguration = ChatConfigurationRule()

    /** `852 − 2 × 64` leaves the design's 724dp column. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a wide window insets the conversation column by the wide-screen padding`() {
        showConversation()

        val column = columnBounds()
        assertEquals(64f, column.left.value, TOLERANCE)
        assertEquals(788f, column.left.value + column.width.value, TOLERANCE)
    }

    /** `411 − 2 × 16` leaves 379dp; the wide-screen padding does not apply to a compact window. */
    @Test
    fun `a compact window insets the conversation column by 16dp`() {
        showConversation()

        val column = columnBounds()
        assertEquals(16f, column.left.value, TOLERANCE)
        assertEquals(395f, column.left.value + column.width.value, TOLERANCE)
    }

    /**
     * Every message item fills the list's width, so any one of them reports the column's own edges.
     *
     * The avatar is drawn outside the bubble through `Modifier.offset`, which takes no room in the item's own
     * bounds, so where it sits relative to the column is a separate measurement — `AudioAttachmentLayoutTest`
     * addresses that node directly.
     */
    private fun columnBounds(): DpRect =
        composeTestRule.onAllNodes(MESSAGE_ITEM)[0].getUnclippedBoundsInRoot()

    private fun showConversation() {
        composeTestRule.setContent { TextConversation() }
        // Idle settles the messages, the mapping running on the dispatcher this rule drives.
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun TextConversation() {
        ChatTheme {
            ChatConversation(
                conversationState = previewUiState(
                    messages = listOf(
                        MessageText(text = "How can I help you today?", direction = ToClient),
                        MessageText(text = "I need a hand with an order.", direction = ToAgent),
                    ),
                    isArchived = false,
                    isLiveChat = false,
                    // The typing indicator is on by default and would take part in the layout.
                    isAgentTyping = false,
                    backgroundDispatcher = dispatcher,
                ),
                audioRecordingState = previewAudioState(),
                onAttachmentTypeSelection = {},
                onError = {},
                showMessageProcessing = false,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 0.5f

        /** Matched on the tag's prefix, because the rest of it is a data class `toString`. */
        val MESSAGE_ITEM = SemanticsMatcher("a message item") {
            it.config.getOrNull(SemanticsProperties.TestTag).orEmpty().startsWith("message_item_")
        }
    }
}
