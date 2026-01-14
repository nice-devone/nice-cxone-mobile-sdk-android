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

package com.nice.cxonechat.ui.data.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.state.FileRestrictions
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Single
import java.util.UUID

/**
 * [ContentDataSource] for videos, pdf documents and other attachments that are
 * treated as raw data.
 *
 * @param context Context for content resolver.
 * @param chatInstanceProvider Provider of Chat instance, to retrieve current accepted mimeTypes.
 */
@Single
internal class DocumentContentDataSource(
    private val context: Context,
    private val chatInstanceProvider: ChatInstanceProvider,
) : ContentDataSource {
    override val acceptRegex: Regex
        get() = getFileRestrictions()

    override fun acceptsMimeType(mimeType: String): Boolean = acceptRegex.containsMatchIn(mimeType)

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
            val suffix = MimeTypeMap
                .getFileExtensionFromUrl(attachmentUri.toString())
                .ifEmpty { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
                ?: return@runInterruptible null
            val fileName = "${UUID.randomUUID()}.$suffix"
            val friendlyName: String = runCatching {
                context.contentResolver.query(
                    attachmentUri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else {
                        fileName
                    }
                } ?: fileName
            }.getOrDefault(fileName)
            ContentDescriptor(
                content = attachmentUri,
                context = context,
                mimeType = mimeType,
                fileName = fileName,
                friendlyName = friendlyName
            )
        }
    }

    private fun getFileRestrictions(): Regex {
        val rawRegex = chatInstanceProvider.chat
            ?.configuration
            ?.fileRestrictions
            ?.let { restrictions ->
                restrictions.allowedFileTypes
                    .takeIf { restrictions.isAttachmentsEnabled }
                    .orEmpty()
                    .map(FileRestrictions.AllowedFileType::mimeType)
            }
            .orEmpty()
            .filter { acceptedMimeTypes.matches(it) }
            .joinToString("|", prefix = "(", postfix = ")")
        return rawRegex.takeIf {
            it.isNotEmpty()
        }?.let { Regex(it) } ?: acceptedMimeTypes
    }

    companion object {
        // The `image/` and `audio/` mime types are handled by the other [ContentDataSource]s.
        private val acceptedMimeTypes = Regex("""(video/.*|application/.*|text/.*)""")
    }
}
