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

import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import com.nice.cxonechat.ui.viewmodel.isAiAgentMessage
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HasAiMessagesPredicateTest {

    private fun hasAiMessages(messages: List<SdkMessage>): Boolean =
        messages.any(::isAiAgentMessage)

    @Test
    fun `returns true when at least one ToClient message has isAiGenerated set`() {
        val messages = listOf(
            UiSdkText(direction = ToClient, isAiGenerated = true),
            UiSdkText(direction = ToClient, isAiGenerated = false),
        )
        assertTrue(hasAiMessages(messages))
    }

    @Test
    fun `returns false when all ToClient messages have isAiGenerated false`() {
        val messages = listOf(
            UiSdkText(direction = ToClient, isAiGenerated = false),
            UiSdkText(direction = ToClient, isAiGenerated = false),
        )
        assertFalse(hasAiMessages(messages))
    }

    @Test
    fun `returns false when ToAgent message has isAiGenerated true`() {
        val messages = listOf(
            UiSdkText(direction = ToAgent, isAiGenerated = true),
        )
        assertFalse(hasAiMessages(messages))
    }

    @Test
    fun `returns false for empty message list`() {
        assertFalse(hasAiMessages(emptyList()))
    }
}
