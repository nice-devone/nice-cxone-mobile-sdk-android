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

@file:Suppress("DataClassShouldBeImmutable")

package com.nice.cxonechat.ui.customvalues

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.state.SelectorNode
import com.nice.cxonechat.state.lookup
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.model.prechat.PreChatResponse

internal sealed interface CustomValueItem<Definition : FieldDefinition, Response> {
    val definition: Definition
    var response: MutableState<Response?>

    fun stringValue(): String?

    data class Text(
        override val definition: FieldDefinition.Text,
        override var response: MutableState<String?>,
    ) : CustomValueItem<FieldDefinition.Text, String> {
        constructor(definition: FieldDefinition.Text, value: String?)
                : this(definition, mutableStateOf(value))

        override fun stringValue() = response.value
    }

    data class Selector(
        override val definition: FieldDefinition.Selector,
        override var response: MutableState<SelectorNode?>,
    ) : CustomValueItem<FieldDefinition.Selector, SelectorNode> {
        constructor(definition: FieldDefinition.Selector, value: String?)
                : this(definition, mutableStateOf(value?.let(definition.values::lookup)))

        override fun stringValue() = response.value?.nodeId
    }

    data class Hierarchy(
        override val definition: FieldDefinition.Hierarchy,
        override var response: MutableState<HierarchyNode<String>?>,
    ) : CustomValueItem<FieldDefinition.Hierarchy, HierarchyNode<String>> {
        constructor(definition: FieldDefinition.Hierarchy, value: String?)
                : this(definition, mutableStateOf(value?.let(definition.values::lookup)))

        override fun stringValue() = response.value?.nodeId
    }

    companion object {
        operator fun invoke(definition: FieldDefinition, value: String? = null) = when (definition) {
            is FieldDefinition.Text -> Text(definition, value)
            is FieldDefinition.Selector -> Selector(definition, value)
            is FieldDefinition.Hierarchy -> Hierarchy(definition, value)
            else -> null
        } as CustomValueItem<*, *>
    }
}

internal typealias CustomValueItemList = List<CustomValueItem<*, *>>

internal fun FieldDefinitionList.mergeWithCustomField(values: List<CustomField>): CustomValueItemList {
    val mapped = values.associate { it.id to it.value }
    return map {
        CustomValueItem(it, mapped[it.fieldId])
    }.toList()
}

/**
 * Convert a [Iterable] of [CustomValueItem]s to a [Map<String, String>] for consumption by [ChatFieldHandler.add()].
 */
internal fun Iterable<CustomValueItem<*, *>>.extractStringValues() =
    mapNotNull { value -> value.stringValue()?.let { value.definition.fieldId to it } }
        .associate { it }

@Suppress("UNCHECKED_CAST")
internal fun CustomValueItem<*, *>.toPreChatResponse(): PreChatResponse? =
    when (val question = this.definition) {
        is FieldDefinition.Text -> (response.value as? String)?.let { value ->
            PreChatSurveyResponse.Text(question, value)
        }

        is FieldDefinition.Selector -> (response.value as? SelectorNode)?.let { value ->
            PreChatSurveyResponse.Selector(question, value)
        }

        is FieldDefinition.Hierarchy -> (response.value as? HierarchyNode<String>)?.let { node ->
            PreChatSurveyResponse.Hierarchy(question, node)
        }

        else -> null
    }
