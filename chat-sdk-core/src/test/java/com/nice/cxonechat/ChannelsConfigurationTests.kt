package com.nice.cxonechat

import com.google.gson.Gson
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.state.FieldDefinition.Hierarchy
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ChannelsConfigurationTests {
    private val channelConfigurationData: String by lazy {
        requireNotNull(ResourceHelper.loadString("channelconfiguration.json"))
    }
    private val serializer: Gson by lazy {
        Default.serializer
    }
    private val configuration: ChannelConfiguration by lazy {
        requireNotNull(serializer.fromJson(channelConfigurationData, ChannelConfiguration::class.java))
    }
    private val published: ConfigurationInternal by lazy {
        configuration.toConfiguration(channelId)
    }

    @Test
    fun testParsing() {
        assertEquals(configuration.contactCustomFields.size, 5)
        assertEquals(configuration.customerCustomFields.size, 4)
    }

    @Test
    fun testPublication() {
        assertEquals(published.contactCustomFields.count(), 5)
        assertEquals(published.customerCustomFields.count(), 4)
    }

    @Suppress("NestedBlockDepth") // verifying hierarchic data just looks better with nested when
    @Test
    fun testHierarchicPublication() {
        val hier = published.contactCustomFields.firstOrNull { it.fieldId == "hie2" } as Hierarchy

        with(hier.values.toList()) {
            assertEquals(2, size)

            with(get(0)) {
                assertEquals("0", label)
                assertEquals(1, children.count())
                with(children.toList()[0]) {
                    assertEquals("0-0", label)
                    assertEquals(2, children.count())
                    with(children.toList()[0]) {
                        assertEquals("0-0-0", label)
                        assertEquals(2, children.count())
                        with(children.toList()[0]) {
                            assertEquals("0-0-0-0", label)
                            assertEquals(0, children.count())
                        }
                        with(children.toList()[1]) {
                            assertEquals("0-0-0-1", label)
                            assertEquals(0, children.count())
                        }
                    }
                    with(children.toList()[1]) {
                        assertEquals("0-0-1", label)
                        assertEquals(0, children.count())
                    }
                }
            }

            with(get(1)) {
                assertEquals("1", label)
                assertEquals(0, children.count())
            }
        }
    }

    companion object {
        const val channelId = ">>Channel Id<<"
    }
}
