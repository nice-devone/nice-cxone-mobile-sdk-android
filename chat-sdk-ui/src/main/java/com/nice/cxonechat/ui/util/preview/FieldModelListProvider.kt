/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.util.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.state.SelectorNode
import com.nice.cxonechat.ui.domain.model.CustomValueItem
import com.nice.cxonechat.ui.domain.model.CustomValueItemList

/*
 * Simple preview of the field list values.
 */
internal class FieldModelListProvider : PreviewParameterProvider<CustomValueItemList> {
    override val values = sequenceOf(
        listOf(
            CustomValueItem.Text(
                object : FieldDefinition.Text {
                    override val fieldId = "name"
                    override val label = "Name"
                    override val isEMail = false
                    override val isRequired = false

                    override fun validate(value: String) = Unit
                },
                "Some Name"
            ),
            CustomValueItem.Text(
                object : FieldDefinition.Text {
                    override val fieldId = "email"
                    override val label = "Email"
                    override val isEMail = true
                    override val isRequired = true

                    override fun validate(value: String) = Unit
                },
                "some.one@some.where"
            ),
            CustomValueItem.Selector(
                object : FieldDefinition.Selector {
                    override val fieldId = "selector"
                    override val label = "Selector"
                    override val isRequired = true
                    override val values = listOf("zero", "one", "one one", "two").mapIndexed { index, label ->
                        object : SelectorNode {
                            override val nodeId = "$index"
                            override val label = label
                        }
                    }.asSequence()

                    override fun validate(value: String) = Unit
                },
                "zero"
            ),
            CustomValueItem.Hierarchy(
                object : FieldDefinition.Hierarchy {
                    override val fieldId = "hierarchy"
                    override val label = "Broken Device"
                    override val isRequired = false
                    override val values = listOf<HierarchyNode<String>>(
                        HierarchyNodeImpl(
                            "Mobile",
                            "0",
                            listOf(
                                HierarchyNodeImpl(
                                    "Android",
                                    "0-0",
                                    listOf(
                                        HierarchyNodeImpl("Samsung", "0-0-0"),
                                        HierarchyNodeImpl("Google", "0-0-1"),
                                    )
                                        .asSequence()
                                ),
                                HierarchyNodeImpl(
                                    "iOS",
                                    "0-1",
                                    listOf(
                                        HierarchyNodeImpl("iPhone 14", "0-1-0"),
                                        HierarchyNodeImpl("iPhone 15", "0-1-1"),
                                    )
                                        .asSequence()
                                )
                            ).asSequence()
                        ),
                        HierarchyNodeImpl("Tablet", "1")
                    ).asSequence()

                    override fun validate(value: String) = Unit
                },
                "0-0-0"
            )
        )
    )
}
