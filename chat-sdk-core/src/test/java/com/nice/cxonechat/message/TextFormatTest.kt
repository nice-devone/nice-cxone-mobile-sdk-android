package com.nice.cxonechat.message

import com.nice.cxonechat.message.TextFormat.Html
import com.nice.cxonechat.message.TextFormat.Markdown
import com.nice.cxonechat.message.TextFormat.Plain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TextFormatTest {
    @Test
    fun testConstruction() {
        assertEquals(Html, TextFormat.from("text/html"))
        assertEquals(Markdown, TextFormat.from("text/markdown"))
        assertEquals(Plain, TextFormat.from("text/plain"))
        assertEquals(Plain, TextFormat.from("text/rtf"))
    }

    @Test
    fun testIsMarkdown() {
        assertTrue(Markdown.isMarkdown)
        assertFalse(Html.isMarkdown)
        assertFalse(Plain.isMarkdown)
    }

    @Test
    fun testIsHtml() {
        assertFalse(Markdown.isHtml)
        assertTrue(Html.isHtml)
        assertFalse(Plain.isHtml)
    }
}
