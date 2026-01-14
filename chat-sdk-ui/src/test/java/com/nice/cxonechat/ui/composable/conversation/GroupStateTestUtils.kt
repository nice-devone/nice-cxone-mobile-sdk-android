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

import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.tool.nextString
import com.nice.cxonechat.ui.util.preview.message.UiSdkMetadata
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import java.util.Date
import java.util.Map.entry

/**
 * Shared test utilities for group state tests.
 */
internal object GroupStateTestUtils {
    fun nextPerson() = Person(
        id = nextString(),
        firstName = nextString(),
        lastName = nextString(),
        imageUrl = null
    )

    fun createTestMessage(status: MessageStatus, sender: Person? = null, createdAt: Date = Date()): Message.Text {
        return Message.Text(
            UiSdkText(
                text = nextString(),
                metadata = UiSdkMetadata(status = status),
                author = sender,
                createdAt = createdAt
            )
        )
    }

    fun createTestEmojiMessage(status: MessageStatus, sender: Person? = null, createdAt: Date = Date()): Message.EmojiText {
        return Message.EmojiText(
            UiSdkText(
                text = "😀",
                metadata = UiSdkMetadata(status = status),
                author = sender,
                createdAt = createdAt
            )
        )
    }

    fun createSection(messages: List<Message>): Section {
        return Section(entry(nextString(), messages))
    }
}
