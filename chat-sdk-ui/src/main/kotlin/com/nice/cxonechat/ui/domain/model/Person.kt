/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        imageUrl = removeDefaultImageUrl(imageUrl)
    )

internal val MessageAuthor.asPerson: Person
    get() = Person(
        id = id,
        firstName = firstName,
        lastName = lastName,
        imageUrl = removeDefaultImageUrl(imageUrl)
    )

/**
 * Builds a [Person] whose [Person.monogram] is derived from an already-resolved display [name]
 * (e.g. `nickname ?: fullName`), matching the iOS reference (cxone-mobile-sdk-ios PR #786): the initials
 * come from the first and last *word* of the resolved name rather than from firstName/lastName directly,
 * so a nickname like "Bombarďák" yields "B" while a resolved "John Doe" yields "JD". Unlike [Agent.asPerson],
 * which always derives initials from firstName/lastName only (used where nickname should NOT affect the
 * monogram, e.g. the ThreadList row), this is for UI that already gives nickname priority in its name text.
 */
internal fun personFromResolvedName(name: String?, imageUrl: String?): Person {
    val words = name?.trim()?.split(WHITESPACE_REGEX)?.filter { it.isNotBlank() }.orEmpty()
    return Person(
        firstName = words.firstOrNull().orEmpty(),
        lastName = words.drop(1).lastOrNull().orEmpty(),
        imageUrl = imageUrl,
    )
}

private val WHITESPACE_REGEX = Regex("\\s+")
private val DEFAULT_IMAGE_URL_REGEX = Regex("(.*/img/user.*\\.png)")

/**
 * Remove default server supplied images used for user avatars.
 * It matches static images which are either a letter or a fallback image.
 *
 * @param url Image url which should be evaluated if it matches expected pattern for a default image.
 * @return Either original [url], or `null` if it is blank or matches the expected default-image pattern.
 */
@VisibleForTesting
internal fun removeDefaultImageUrl(url: String?): String? =
    if (url.isNullOrBlank() || DEFAULT_IMAGE_URL_REGEX.matches(url)) null else url
