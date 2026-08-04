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

package com.nice.cxonechat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string

internal val Attachment.contentDescription: String?
    get() = friendlyName.ifBlank {
        runCatching {
            url.toUri().lastPathSegment
        }.getOrNull()
    }

internal val Attachment.typeAndName: String
    @Composable get() {
        val mimeType = mimeTypeToDescription()
        val name = contentDescription
        return if (name.isNullOrBlank()) mimeType else stringResource(string.accessibility_attachment_type_and_name, mimeType, name)
    }

@Composable
internal fun Attachment.mimeTypeToDescription(): String {
    val type = mimeType.orEmpty()
    return when {
        type.startsWith("image/", ignoreCase = true) -> stringResource(string.content_description_attachment_type_image)
        type.startsWith("video/", ignoreCase = true) -> stringResource(string.content_description_attachment_type_video)
        type.startsWith("application/pdf", ignoreCase = true) -> stringResource(string.content_description_attachment_type_document)
        else -> stringResource(string.content_description_attachment_type_file)
    }
}
