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

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.net.Uri
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.message.ContentDescriptor
import org.koin.core.annotation.Single

/**
 * List of available [ContentDataSource] which can be used
 * to process an content URI for attachment.
 *
 * @param context for content resolving
 * @param chatInstanceProvider Provider of Chat instance
 * @param imageContentDataSource [ContentDataSource] for images
 * @param documentContentDataSource [ContentDataSource] for videos and pdf
 * documents and other attachments that are treated as raw data
 * @param audioContentDataSource [ContentDataSource] for audio
 */
@Single
internal class ContentDataSourceList(
    private val context: Context,
    private val chatInstanceProvider: ChatInstanceProvider,
    imageContentDataSource: ImageContentDataSource,
    documentContentDataSource: DocumentContentDataSource,
    audioContentDataSource: MediaStoreAudioContentDataSource,
) {
    private val dataSources = listOf(
        imageContentDataSource,
        documentContentDataSource,
        audioContentDataSource,
    )

    private val availableMemory
        get() = runCatching {
            MemoryInfo()
                .also(context.getSystemService(ActivityManager::class.java)::getMemoryInfo)
                .availMem
        }

    private val allowedFileSize
        get() = runCatching {
            requireNotNull(
                chatInstanceProvider
                    .chat
                    ?.configuration
                    ?.fileRestrictions
                    ?.allowedFileSize
                    ?.times(1024L * 1024L)
            )
        }

    /**
     * Search available data sources for one that can handle the requested uri.
     * The requested uri, is first validated if it matches file size restrictions.
     *
     * @param uri attachment URI to process
     * @return [ContentRequestResult.Success] for attachment upload if an appropriate data source
     * was found. [ContentRequestResult.Error] if no data source could be found, data was too large or there was error
     * during it's retrieval.
     */
    @Suppress(
        "ReturnCount"
    )
    suspend fun descriptorForUri(uri: Uri): ContentRequestResult {
        if (!checkFileSize(uri)) return ContentRequestResult.ContentTooLarge
        val dataSource = getDatasourceForContent(uri) ?: return ContentRequestResult.UnsupportedContentType
        return dataSource.descriptorForUri(uri)?.let(ContentRequestResult::Success) ?: ContentRequestResult.ErrorRetrievingContent
    }

    private fun checkFileSize(uri: Uri): Boolean {
        val allowedFileSizeResult = allowedFileSize
        val maxSize = if (allowedFileSizeResult.isFailure) {
            availableMemory
        } else {
            runCatching { minOf(availableMemory.getOrThrow(), allowedFileSizeResult.getOrThrow()) }
        }
        val fileSize = getFileSize(uri)
        return fileSize.isFailure || maxSize.isFailure || fileSize.getOrThrow() <= maxSize.getOrThrow()
    }

    private fun getFileSize(uri: Uri) = runCatching {
        requireNotNull(
            context
                .contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { fd ->
                    fd.statSize
                }
        )
    }

    private fun getDatasourceForContent(uri: Uri): ContentDataSource? {
        val mimeType = context.contentResolver.getType(uri) ?: return null
        return dataSources.firstOrNull { dataSource -> dataSource.acceptsMimeType(mimeType) }
    }

    sealed interface ContentRequestResult {
        @JvmInline
        value class Success(val content: ContentDescriptor) : ContentRequestResult

        sealed interface Error : ContentRequestResult
        data object ContentTooLarge : Error
        data object UnsupportedContentType : Error
        data object ErrorRetrievingContent : Error
    }
}
