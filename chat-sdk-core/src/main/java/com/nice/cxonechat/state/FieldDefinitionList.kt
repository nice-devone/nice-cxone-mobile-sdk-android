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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import com.nice.cxonechat.exceptions.UndefinedCustomField

/** Type alias for a list of [FieldDefinition]. */
@Public
typealias FieldDefinitionList = Sequence<FieldDefinition>

/**
 * Search target [FieldDefinitionList] for a [FieldDefinition] item with the requested fieldId.
 *
 * @receiver [FieldDefinitionList] to search.
 * @param fieldId Field ID to seek.
 * @return [FieldDefinition] matching [fieldId] or null if none is found.
 */
@Public
fun FieldDefinitionList.lookup(fieldId: String): FieldDefinition? = firstOrNull { it.fieldId == fieldId }

/**
 * Test target [FieldDefinitionList] for a [FieldDefinition] with a matching fieldId.
 *
 * @receiver [FieldDefinitionList] to search.
 * @param fieldId Field ID to seek.
 * @return true iff receiver contains a matching [FieldDefinition].
 */
@Public
fun FieldDefinitionList.containsField(fieldId: String): Boolean = map { it.fieldId }.contains(fieldId)

/**
 * Validate a map of proposed fieldId, value pairs against the target
 * [FieldDefinitionList].
 *
 * The following checks are performed:
 * * all mentioned fieldId's must have a definition in the receiver
 * * [FieldDefinition.Text] fields will be checked for a proper email format if non-blank
 * and isEMail is set
 * * [FieldDefinition.Selector] and [FieldDefinition.Hierarchy]
 * fields will be checked to verify that the value matches an
 * included node [SelectorNode.nodeId] or [HierarchyNode.nodeId].
 * * [FieldDefinition.Hierarchy] fields will verify the selected node is a leaf node.
 * @throws UndefinedCustomField if the above-mentioned checks don't pass for any value.
 */
@Throws(
    UndefinedCustomField::class
)
@Public
fun FieldDefinitionList.validate(values: Map<String, String>) {
    values.asSequence().forEach { (fieldId, value) ->
        if (value.isNotBlank()) {
            firstOrNull { it.fieldId == fieldId }
                ?.validate(value)
                ?: throw UndefinedCustomField(fieldId)
        }
    }
}

/**
 * Check that all required fields in the list have a value specified in [values]
 *
 * Each field flag in receiver that is marked `isRequired` must have a matching
 * non-blank value in values.
 *
 * @receiver [FieldDefinitionList] to validate
 * @param values Map of fieldIds and values to check.
 * @throws MissingPreChatCustomFieldsException if any fields are missing.
 */
@Throws(
    MissingPreChatCustomFieldsException::class
)
@Public
fun FieldDefinitionList.checkRequired(values: Map<String, String>) {
    filter(FieldDefinition::isRequired)
        .filter { values[it.fieldId]?.isBlank() != false }
        .map { it.label }
        .let(Sequence<String>::toList)
        .run {
            if (isNotEmpty()) {
                throw MissingPreChatCustomFieldsException(this)
            }
        }
}
