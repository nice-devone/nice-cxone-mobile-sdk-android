/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.message.ContentDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * List of available [ContentDataSource] which can be used
 * to process an content URI for attachment.
 *
 * @property context for content resolving
 * @param imageContentDataSource [ContentDataSource] for images
 * @param documentContentDataSource [ContentDataSource] for videos and pdf
 * documents and other attachments that are treated as raw data
 * @param audioContentDataSource [ContentDataSource] for audio
 */
@Singleton
internal class ContentDataSourceList @Inject constructor(
    @ApplicationContext private val context: Context,
    imageContentDataSource: ImageContentDataSource,
    documentContentDataSource: DocumentContentDataSource,
    audioContentDataSource: MediaStoreAudioContentDataSource,
) {
    private val dataSources = listOf(
        imageContentDataSource,
        documentContentDataSource,
        audioContentDataSource,
    )

    /**
     * Search available data sources for one that can handle the requested uri.
     *
     * @param uri attachment URI to process
     * @return [ContentDescriptor] for attachment upload if an appropriate data source
     * was found.  null if no data source could be found
     */
    suspend fun descriptorForUri(uri: Uri): ContentDescriptor? {
        val mimeType = context.contentResolver.getType(uri) ?: return null

        return dataSources
                .firstOrNull { it.acceptsMimeType(mimeType) }
                ?.descriptorForUri(uri)
    }
}
