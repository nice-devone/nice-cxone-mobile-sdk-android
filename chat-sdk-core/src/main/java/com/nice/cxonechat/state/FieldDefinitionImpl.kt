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

import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.internal.model.CustomFieldPolyType
import com.nice.cxonechat.internal.model.PreContactCustomFieldDefinitionModel
import com.nice.cxonechat.state.HierarchyNodeInternal.Companion.toNodeIterable
import java.util.regex.Pattern

internal sealed class FieldDefinitionImpl: FieldDefinition {
    internal data class Text(
        override val fieldId: String,
        override val label: String,
        override val isEMail: Boolean,
        override val isRequired: Boolean,
    ) : FieldDefinition.Text {
        constructor(details: CustomFieldPolyType.Text, isEMail: Boolean, isRequired: Boolean = false) : this(
            fieldId = details.fieldId,
            label = details.label,
            isEMail = isEMail,
            isRequired = isRequired,
        )

        constructor(details: CustomFieldPolyType.Email, isEMail: Boolean, isRequired: Boolean = false) : this(
            fieldId = details.fieldId,
            label = details.label,
            isEMail = isEMail,
            isRequired = isRequired,
        )

        override fun validate(value: String) {
            if(isEMail && !EMAIL_ADDRESS.matcher(value).matches()) {
                throw InvalidCustomFieldValue(label, "Invalid email address")
            }
        }

        override fun toString() =
            "Text(fieldId='$fieldId', label='$label', isEMail=$isEMail, isRequired=$isRequired)"
    }

    internal data class Selector(
        override val fieldId: String,
        override val label: String,
        override val values: Sequence<SelectorNode>,
        override val isRequired: Boolean,
    ) : FieldDefinition.Selector {
        constructor(details: CustomFieldPolyType.Selector, isRequired: Boolean = false) : this(
            fieldId = details.fieldId,
            label = details.label,
            values = details.values.map { orig -> SelectorNodeImpl(orig.name, orig.label) }.asSequence(),
            isRequired = isRequired,
        )

        override fun validate(value: String) {
            if (!values.contains(value)) throw InvalidCustomFieldValue(label, "Illegal selector value")
        }

        override fun toString() =
            "Selector(fieldId='$fieldId', label='$label', values=$values, isRequired=$isRequired)"
    }

    internal data class Hierarchy(
        override val fieldId: String,
        override val label: String,
        override val values: Sequence<HierarchyNode<String>>,
        override val isRequired: Boolean,
    ) : FieldDefinition.Hierarchy {
        constructor(details: CustomFieldPolyType.Hierarchy, isRequired: Boolean = false) : this(
            fieldId = details.fieldId,
            label = details.label,
            values = details.values.toNodeIterable(),
            isRequired = isRequired,
        )

        override fun validate(value: String) {
            if(values.lookup(value)?.isLeaf != true) {
                throw InvalidCustomFieldValue(label, "Illegal hierarchy value")
            }
        }

        override fun toString() =
            "Hierarchy(fieldId='$fieldId', label='$label', values=$values, isRequired=$isRequired)"
    }

    companion object {
        // this is declared here as a duplicate of android.utils.Patterns to enable testing
        // via junit, which doesn't supply implementations for android.* classes and there
        // doesn't seem to be any good way of mocking static fields.
        internal val EMAIL_ADDRESS: Pattern by lazy {
            Pattern.compile(
                """[a-zA-Z\d._%-+]{1,256}@[a-zA-Z\d][a-zA-Z\d\-]{0,64}(\.[a-zA-Z\d][a-zA-Z\d\-]{0,25})+"""
            )
        }

        internal operator fun invoke(source: CustomFieldPolyType, isRequired: Boolean = false) = when(source) {
            is CustomFieldPolyType.Text -> Text(source, isEMail = false, isRequired = isRequired)
            is CustomFieldPolyType.Email -> Text(source, isEMail = true, isRequired = isRequired)
            is CustomFieldPolyType.Selector -> Selector(source, isRequired = isRequired)
            is CustomFieldPolyType.Hierarchy -> Hierarchy(source, isRequired = isRequired)
            is CustomFieldPolyType.Noop -> null
        }

        internal operator fun invoke(source: PreContactCustomFieldDefinitionModel) =
            FieldDefinitionImpl(source.definition, source.isRequired)
    }
}
