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

@file:Suppress(
    "FunctionMaxLength", // Tests can have long function names.
)

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.ThreadListFetched
import com.nice.cxonechat.enums.EventType.ThreadRecovered
import com.nice.cxonechat.internal.socket.EventBlueprint.Postback
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class EventBlueprintTest {

    private lateinit var blueprint: EventBlueprint

    @Before
    fun prepare() {
        blueprint = EventBlueprint(null, null)
    }

    @Test
    fun anyType_returnsTypeWhenSet() {
        val expected = EventType.values().random()
        val blueprint = blueprint.setType(expected)
        assertEquals(blueprint.type, blueprint.anyType)
    }

    @Test
    fun anyType_returnsPostbackTypeWhenSet() {
        val expected = EventType.values().random()
        val blueprint = blueprint.setPostbackType(expected)
        assertEquals(blueprint.postback?.type, blueprint.anyType)
    }

    @Test
    fun anyType_givesPriorityToEvent() {
        val expected = ThreadRecovered
        val blueprint = blueprint.copy(
            type = expected,
            postback = Postback(ThreadListFetched)
        )
        assertEquals(expected, blueprint.anyType)
    }

    // ---

    private fun EventBlueprint.setType(type: EventType) =
        copy(type = type)

    private fun EventBlueprint.setPostbackType(type: EventType) =
        copy(postback = Postback(type = type))
}
