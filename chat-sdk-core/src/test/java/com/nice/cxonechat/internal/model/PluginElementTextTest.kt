package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement.Text
import com.nice.cxonechat.message.TextFormat
import com.nice.cxonechat.message.TextFormat.Html
import com.nice.cxonechat.message.TextFormat.Markdown
import com.nice.cxonechat.message.TextFormat.Plain
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

class PluginElementTextTest {
    private fun pluginElement(text: String, format: TextFormat) = PluginElementText(
        Text(text, format.mimeType)
    )

    @Test
    fun getFormat() {
        assertEquals(Plain, pluginElement("text", Plain).format)
        assertEquals(Markdown, pluginElement("text", Markdown).format)
        assertEquals(Html, pluginElement("text", Html).format)
    }

    @Suppress("DEPRECATION")
    @Test
    fun isMarkdown() {
        assertFalse(pluginElement("text", Plain).isMarkdown)
        assertTrue(pluginElement("text", Markdown).isMarkdown)
        assertFalse(pluginElement("text", Html).isMarkdown)
    }

    @Suppress("DEPRECATION")
    @Test
    fun isHtml() {
        assertFalse(pluginElement("text", Plain).isHtml)
        assertFalse(pluginElement("text", Markdown).isHtml)
        assertTrue(pluginElement("text", Html).isHtml)
    }
}
