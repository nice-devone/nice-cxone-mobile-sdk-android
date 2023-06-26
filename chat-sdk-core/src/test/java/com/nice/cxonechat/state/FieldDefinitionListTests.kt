package com.nice.cxonechat.state

import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("StringLiteralDuplication")
internal class FieldDefinitionListTests {
    @Test
    fun testLookup() {
        with(list.lookup(text)) {
            assertNotNull(this)
            assertEquals(text, fieldId)
            assertEquals("Text", label)
        }

        assertNull(list.lookup("field3"))
    }

    @Test
    fun testContains() {
        assertTrue(list.containsField(text))
        assertTrue(list.containsField(email))
        assertFalse(list.containsField("field3"))
    }

    @Test
    fun textInvalidNonEmailNoThrow() {
        list.validate(
            mapOf(
                text to "foo"
            )
        )
    }

    @Test
    fun emailValidNoThrow() {
        list.validate(
            mapOf(
                text to "test.user@hoe.down"
            )
        )
    }

    @Test(expected = InvalidCustomFieldValue::class)
    @Ignore("Doesn't work in current test environment because email validation requires Android environment.")
    fun emailInvalidThrows() {
        list.validate(
            mapOf(
                email to "test.user@"
            )
        )
    }

    @Test
    fun selectorIncludedNoThrow() {
        list.validate(mapOf(selector to "item1"))
    }

    @Test(expected = InvalidCustomFieldValue::class)
    fun selectorNotIncludedThrows() {
        list.validate(mapOf(selector to "item2"))
    }

    @Test
    fun hierarchyIncludedNoThrow() {
        list.validate(mapOf(hierarchy to "0-0"))
    }

    @Test(expected = InvalidCustomFieldValue::class)
    fun hierarchyNotIncludedThrows() {
        list.validate(mapOf(selector to "1"))
    }

    @Test(expected = InvalidCustomFieldValue::class)
    fun hierarchyNotLeafThrows() {
        list.validate(mapOf(selector to "0"))
    }

    fun allRequiredNoThrow() {
        list.checkRequired(
            mapOf(
                text to "text",
                email to "email",
                selector to "selector",
                hierarchy to "hierarchy"
            )
        )
    }

    @Test
    fun missingRequiredThrows() {
        var thrown = false

        try {
            list.checkRequired(
                mapOf(
                    text to "text",
                    email to "email",
                    selector to "",
                )
            )
        } catch(exc: MissingPreChatCustomFieldsException) {
            /* list should include selector because it's blank, hierarchy because it's missing */
            assertEquals(listOf("Selector", "Hierarchy"), exc.missing)
            thrown = true
        }

        assertTrue(thrown, "Expected exception not thrown")
    }

    companion object {
        const val text = "text"
        const val email = "email"
        const val selector = "selector"
        const val hierarchy = "hierarchy"

        private val list: FieldDefinitionList = sequenceOf(
            FieldDefinitionImpl.Text(text, "Text", isEMail = false, isRequired = true),
            FieldDefinitionImpl.Text(email, "EMail", isEMail = true, isRequired = true),
            FieldDefinitionImpl.Selector(
                selector,
                "Selector",
                sequenceOf(
                    SelectorNodeImpl("item1", "Item 1")
                ),
                isRequired = true
            ),
            FieldDefinitionImpl.Hierarchy(
                hierarchy,
                "Hierarchy",
                sequenceOf(
                    HierarchyNodeInternal(
                        "0",
                        "Item 0",
                        mutableListOf(
                            HierarchyNodeInternal("0-0", "Item 0-0"),
                        )
                    ),
                ),
                isRequired = true
            )
        )
    }
}
