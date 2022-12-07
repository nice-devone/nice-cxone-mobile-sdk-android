@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat.socket

import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.ThreadListFetched
import com.nice.cxonechat.enums.EventType.ThreadRecovered
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
            postback = EventBlueprint.Postback(ThreadListFetched)
        )
        assertEquals(expected, blueprint.anyType)
    }

    // ---

    private fun EventBlueprint.setType(type: EventType) =
        copy(type = type)

    private fun EventBlueprint.setPostbackType(type: EventType) =
        copy(postback = EventBlueprint.Postback(type = type))

}
