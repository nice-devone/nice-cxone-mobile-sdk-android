/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.message

import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.makeUserStatistics
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals

internal class MessageMetadataStatusTest {

    @Test
    fun message_is_sent_by_default() {
        val defaultMessage: Message = makeMessage()
        assertEquals(MessageStatus.Sent, defaultMessage.metadata.status)
    }

    @Test
    fun message_is_reported_as_seen() {
        val messageSeen: Message = makeMessage(
            model = makeMessageModel(
                userStatistics = makeUserStatistics(
                    seenAt = Date()
                )
            )
        )
        assertEquals(MessageStatus.Seen, messageSeen.metadata.status)
    }

    @Test
    fun message_is_reported_as_read() {
        val messageSeenAndRead: Message = makeMessage(
            model = makeMessageModel(
                userStatistics = makeUserStatistics(
                    seenAt = Date(),
                    readAt = Date()
                )
            )
        )
        assertEquals(MessageStatus.Read, messageSeenAndRead.metadata.status)
        val messageRead: Message = makeMessage(
            model = makeMessageModel(
                userStatistics = makeUserStatistics(
                    readAt = Date()
                )
            )
        )
        assertEquals(MessageStatus.Read, messageRead.metadata.status)
    }
}
