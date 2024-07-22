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
import android.provider.MediaStore
import com.nice.cxonechat.message.ContentDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * [ContentDataSource] for media audio files based on android data storage.
 *
 * @param context Application context used for queries to ContentResolver.
 */
@Single
internal class MediaStoreAudioContentDataSource(
    private val context: Context,
) : ContentDataSource {
    override val acceptRegex: Regex = "audio/.*".toRegex()

    override suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor? = withContext(Dispatchers.IO) {
        context.contentResolver
            .query(attachmentUri, null, null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                cursor.moveToFirst()
                ContentDescriptor(
                    content = attachmentUri,
                    context = context,
                    mimeType = cursor.getString(mimeTypeIndex),
                    fileName = cursor.getString(nameIndex),
                )
            }
    }
}
