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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextString
import org.junit.After
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatActionHandlerTest : AbstractChatTest() {

    private lateinit var actions: ChatActionHandler

    override fun prepare() {
        super.prepare()
        actions = chat.actions()
    }

    @After
    fun tearDown() {
        actions.close()
    }

    @Test
    fun onPopup_notifies_ofNewPopups() {
        val expected = mapOf("param" to "my param value")
        val (params) = testCallback(::onPopup) {
            sendServerMessage(ServerResponse.ActionPopup(expected))
        }
        assertEquals(expected, params)
    }

    @Test
    fun onPopup_notifies_ofLatestUnhandledPopup() {
        val expected = mapOf("param" to "my param value")
        this serverResponds ServerResponse.ActionPopup(expected)
        val (params) = testCallback(::onPopup)
        assertEquals(expected, params)
    }

    @Test
    fun onPopup_doesNotNotify_whenHandledCallbackPreviously() {
        this serverResponds ServerResponse.ActionPopup(emptyMap())
        testCallback(::onPopup)
        val result = testCallback(::onPopup)
        assertNull(result)
    }

    @Test
    fun onPopup_returns_metadata() {
        val id = UUID.randomUUID()
        val name = nextString()
        val type = ActionType.CustomPopupBox
        val (_, meta) = testCallback(::onPopup) {
            val popup = ServerResponse.ActionPopup(
                params = emptyMap(),
                actionId = id,
                actionName = name,
                actionType = type
            )
            sendServerMessage(popup)
        }
        val expected = ActionMetadataInternal(id, name, type)
        assertEquals(expected, meta)
    }

    // ---

    private fun onPopup(callback: (Pair<Map<String, Any?>, ActionMetadata>) -> Unit) {
        actions.onPopup { params, meta -> callback(params to meta) }
    }
}
