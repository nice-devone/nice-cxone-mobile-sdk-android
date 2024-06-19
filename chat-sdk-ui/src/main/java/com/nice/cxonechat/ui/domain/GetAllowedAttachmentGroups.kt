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

package com.nice.cxonechat.ui.domain

import com.nice.cxonechat.ui.data.AllowedFileType

internal object GetAllowedAttachmentGroups {

    private val FULL_MIMETYPE_REGEX = Regex(".+/.+")

    /**
     * Associates [AllowedFileType]s with supported mime type groups (and their labels).
     *
     * @param labels Labels for the [supportedMimeTypeGroups].
     * @param supportedMimeTypeGroups String templates for [Regex] defining a supported MimeType group.
     * @param allowedFileTypes [AllowedFileType]s by the Chat SDK configuration.
     */
    fun allowedAttachmentGroups(
        labels: Iterable<String>,
        supportedMimeTypeGroups: Iterable<String>,
        allowedFileTypes: List<AllowedFileType>,
    ): Iterable<MimeTypeGroup> {
        val regexOpts = supportedMimeTypeGroups.map(::Regex).zip(labels)
        val allowedMimeTypes = allowedFileTypes.map(AllowedFileType::mimeType).toMutableList()
        return regexOpts.map { (regex, label) ->
            val matching = allowedMimeTypes.filter { type ->
                regex.matchEntire(type) != null
            }
            allowedMimeTypes.removeAll(matching)
            val verifiedMatching = matching.map { it.addSubtypeIfMissing() }.toSet()
            MimeTypeGroup(label, verifiedMatching)
        }.filterNot {
            it.options.isEmpty()
        }
    }

    private fun String.addSubtypeIfMissing() = if (FULL_MIMETYPE_REGEX.matchEntire(this) != null) this else "$this/*"
}

/**
 * Holds label for a group of mime types.
 */
internal data class MimeTypeGroup(
    val label: String,
    val options: Collection<String>,
)
