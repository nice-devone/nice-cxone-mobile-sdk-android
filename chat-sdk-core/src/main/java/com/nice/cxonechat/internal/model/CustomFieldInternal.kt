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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.thread.CustomField
import java.util.Date

internal data class CustomFieldInternal(
    override val id: String,
    override val value: String,
    override val updatedAt: Date,
) : CustomField {

    constructor(
        entry: Map.Entry<String, String>,
    ) : this(
        id = entry.key,
        value = entry.value,
        updatedAt = Date(),
    )

    override fun toString(): String = "CustomField(" +
            "id='$id', " +
            "value='$value', " +
            "updatedAt=$updatedAt" +
            ")"

    internal companion object {

        /**
         * Merges the given list of [CustomField]s with [updatedList] list, preserving only most current values for given id.
         * In case of matching [CustomField.updatedAt] timestamp the [updatedList] value will be preserved.
         */
        internal fun List<CustomField>.updateWith(updatedList: List<CustomField>): List<CustomField> = updatedList
            .plus(this)
            .sortedWith(CustomFieldComparator)
            .distinctBy { it.id }

        /**
         * Comparator which sorts [CustomField] by [CustomField.id] first
         * and in case of compares by [CustomField.updatedAt] where a result
         * of the normal compare function is reversed.
         * [CustomField.value] is left out of the comparison intentionally.
         */
        private object CustomFieldComparator : Comparator<CustomField> {
            override fun compare(o1: CustomField, o2: CustomField): Int {
                val idCompare = o1.id.compareTo(o2.id)
                return if (idCompare != 0) {
                    idCompare
                } else {
                    -o1.updatedAt.compareTo(o2.updatedAt)
                }
            }
        }
    }
}
