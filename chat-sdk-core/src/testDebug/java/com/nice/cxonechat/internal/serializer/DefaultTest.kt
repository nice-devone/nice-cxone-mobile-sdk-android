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

@file:Suppress("unused")

package com.nice.cxonechat.internal.serializer

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.tool.serialize
import org.junit.Test
import kotlin.test.assertEquals

internal class DefaultTest {

    /**
     * Verifies fix for DE-53160.
     */
    @Test
    fun verify_lenient_UUID_parsing_for_empty_strings() {
        val expectedAgent = makeAgent(inContactId = null)
        val agent = object {
            val id = expectedAgent.id
            val inContactId = ""
            val emailAddress: String? = expectedAgent.emailAddress
            val firstName: String = expectedAgent.firstName
            val surname: String = expectedAgent.surname
            val nickname: String? = expectedAgent.nickname
            val isBotUser: Boolean = expectedAgent.isBotUser
            val isSurveyUser: Boolean = expectedAgent.isSurveyUser
            val imageUrl: String = expectedAgent.imageUrl
        }
        val serializedAgent = agent.serialize()
        assertEquals(expectedAgent, Default.serializer.fromJson(serializedAgent, AgentModel::class.java))
    }
}
