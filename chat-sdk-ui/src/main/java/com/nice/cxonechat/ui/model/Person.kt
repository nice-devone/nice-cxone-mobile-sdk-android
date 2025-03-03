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

package com.nice.cxonechat.ui.model

import androidx.compose.runtime.Immutable
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent

@Immutable
internal data class Person(
    override val id: String = "",
    override val firstName: String = "",
    override val lastName: String = "",
    override val imageUrl: String? = null
): MessageAuthor() {
    val monogram: String? = listOf(firstName, lastName)
        .mapNotNull { it.firstOrNull() }
        .joinToString(separator = "")
        .ifBlank { null }

    val fullName: String? = listOf(firstName, lastName)
            .mapNotNull { it.ifBlank { null } }
            .joinToString(separator = " ")
            .ifBlank { null }
}

internal val Agent.asPerson: Person
    get() = Person(
        id = id.toString(),
        firstName = firstName,
        lastName = lastName,
        imageUrl = imageUrl
    )

internal val MessageAuthor.asPerson: Person
    get() = Person(
        id = id,
        firstName = firstName,
        lastName = lastName,
        imageUrl = imageUrl
    )
