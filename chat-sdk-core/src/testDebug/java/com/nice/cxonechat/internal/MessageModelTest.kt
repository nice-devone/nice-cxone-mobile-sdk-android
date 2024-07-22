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

package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.MessageDirectionModel
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.MessageModel
import io.mockk.mockk
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals

internal class MessageModelTest {
    private fun messageModel(direction: MessageDirectionModel) = MessageModel(
        idOnExternalPlatform = UUID.randomUUID(),
        threadIdOnExternalPlatform = UUID.randomUUID(),
        messageContent = mockk(),
        createdAt = Date(),
        attachments = listOf(),
        direction = direction,
        userStatistics = mockk(),
        authorUser = AgentModel(
            id = 1,
            inContactId = UUID.randomUUID(),
            emailAddress = null,
            firstName = "Agent",
            surname = "Name",
            nickname = null,
            isBotUser = false,
            isSurveyUser = false,
            imageUrl = "http://doesnt.exist",
        ),
        authorEndUserIdentity = CustomerIdentityModel(
            idOnExternalPlatform = UUID.randomUUID().toString(),
            firstName = "Customer",
            lastName = "Name",
            imageUrl = null,
        )
    )

    @Test
    fun testClientAuthor() {
        assertEquals(
            messageModel(direction = ToAgent).author?.firstName,
            "Customer"
        )
        assertEquals(
            messageModel(direction = ToClient).author?.firstName,
            "Agent"
        )
    }
}
