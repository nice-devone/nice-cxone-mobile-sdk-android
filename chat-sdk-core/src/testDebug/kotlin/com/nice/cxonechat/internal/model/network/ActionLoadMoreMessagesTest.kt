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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.tool.serialize
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

internal class ActionLoadMoreMessagesTest {

    @Test
    fun `oldestMessageDatetime is truncated to whole seconds when serialized`() {
        val action = ActionLoadMoreMessages(
            connection = makeConnection(),
            thread = makeChatThread(
                scrollToken = "token",
                messages = listOf(
                    makeMessage(makeMessageModel(createdAt = Instant.parse("2026-07-01T12:34:56.789Z"))),
                ),
            ),
        )

        assertEquals("2026-07-01T12:34:56Z", action.oldestMessageDatetime())
    }

    private fun ActionLoadMoreMessages.oldestMessageDatetime(): String =
        Default.serializer.parseToJsonElement(serialize())
            .jsonObject.getValue("payload")
            .jsonObject.getValue("data")
            .jsonObject.getValue("oldestMessageDatetime")
            .jsonPrimitive.content
}
