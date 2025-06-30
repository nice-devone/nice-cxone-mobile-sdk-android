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

package com.nice.cxonechat.ui.domain.model

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent
import java.util.Locale

/**
 * Represents a person in the chat system, extending from [MessageAuthor].
 * This class provides additional properties such as `monogram` and `fullName`.
 *
 * @property id Unique identifier for the person.
 * @property firstName First name of the person.
 * @property lastName Last name of the person.
 * @property imageUrl Optional URL for the person's image.
 */
@Immutable
internal data class Person(
    override val id: String = "",
    override val firstName: String = "",
    override val lastName: String = "",
    override val imageUrl: String? = null,
) : MessageAuthor() {
    val monogram: String? = listOf(firstName, lastName)
        .mapNotNull { it.firstOrNull()?.uppercase(Locale.getDefault()) }
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
        imageUrl = removeDefaultImageUrl(imageUrl)
    )

internal val MessageAuthor.asPerson: Person
    get() = Person(
        id = id,
        firstName = firstName,
        lastName = lastName,
        imageUrl = removeDefaultImageUrl(imageUrl)
    )

private const val DEFAULT_IMAGE_URL = "(.*/img/user.*\\.png)"

/**
 * Remove default server supplied images used for user avatars.
 * It matches static images which are either a letter or a fallback image.
 *
 * @param url Image url which should be evaluated if it matches expected pattern for a default image.
 * @return Either original [url] or `null` iff it matches expected pattern.
 */
@VisibleForTesting
internal fun removeDefaultImageUrl(url: String?): String? =
    if (url == null || DEFAULT_IMAGE_URL.toRegex().matches(url)) null else url
