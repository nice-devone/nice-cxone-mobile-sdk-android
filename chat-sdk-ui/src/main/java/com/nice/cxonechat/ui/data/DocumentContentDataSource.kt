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

package com.nice.cxonechat.ui.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nice.cxonechat.message.ContentDescriptor
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.util.UUID

/**
 * [ContentDataSource] for videos, pdf documents and other attachments that are
 * treated as raw data.
 *
 * @param context Context for content resolver
 */
@Single
@Module()
internal class DocumentContentDataSource(
    private val context: Context,
) : ContentDataSource {
    override val acceptRegex = Regex("""(video/.*|application/pdf)""")

    /**
     * fetch the details (primarily the content) of a content URI
     * for an attachment.
     *
     * @param attachmentUri uri to process
     * @return details of uri prepared for uploading
     */
    override suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor? {
        return runInterruptible {
            val mimeType = context.contentResolver.getType(attachmentUri) ?: return@runInterruptible null
            val suffix = MimeTypeMap.getFileExtensionFromUrl(attachmentUri.toString()) ?: return@runInterruptible null

            ContentDescriptor(
                content = attachmentUri,
                context = context,
                mimeType = mimeType,
                fileName = "${UUID.randomUUID()}.$suffix",
                friendlyName = attachmentUri.lastPathSegment
            )
        }
    }
}
