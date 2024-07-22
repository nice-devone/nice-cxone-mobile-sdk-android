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
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nice.cxonechat.message.ContentDescriptor
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Single
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/**
 * [ContentDataSource] that handles Image uri's for uploading to the host.
 *
 * Any image to be uploaded will converted to jpeg with a quality of 90
 * before a [ContentDescriptor] is created.  This means that a fairly large
 * chunk of memory will be consumed while the ContentDescriptor is held.
 *
 * @param context Context to be used for Uri resolution
 */
@Single
internal class ImageContentDataSource(
    private val context: Context,
) : ContentDataSource {
    override val acceptRegex = Regex("image/.*")

    /**
     * Create a [ByteArray]-based [ContentDescriptor] by reading an image
     * from [attachmentUri] and compressing it as JPEG.
     *
     * @param attachmentUri image [Uri] to process
     * @return [ContentDescriptor] suitable for passing to the SDK for upload or
     * null if the file could not be found, read, or converted.
     */
    override suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor? {
        return runInterruptible {
            try {
                ContentDescriptor(
                    // we read the attached image here, rather than postponing it so we can
                    // can convert it to JPG from whatever it happens to currently be, which
                    // is probably either PNG or WEBP
                    content = getContent(attachmentUri) ?: return@runInterruptible null,
                    mimeType = "image/jpeg",
                    fileName = "${UUID.randomUUID()}.jpg",
                    friendlyName = attachmentUri.lastPathSegment
                )
            } catch(_: IOException) {
                null
            }
        }
    }

    /**
     * Read the content of an image [Uri].
     *
     * The image at [Uri] will be read as a [Bitmap], converted to JPEG, and returned as a [ByteArray].
     *
     * @param imageUri [Uri] of image to read
     * @return [ByteArray] of the image contained at [Uri]
     * @throws FileNotFoundException if the Uri does not reference a valid file
     * @throws IOException if an error occurs while reading the file
     */
    @Throws(FileNotFoundException::class, IOException::class)
    private fun getContent(imageUri: Uri) =
        context.bitmapForUri(imageUri)?.let { bitmap ->
            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.toByteArray()
            }
        }

    /**
     * Read data from [uri] using the receiver [Context] and create a [Bitmap] of the result.
     *
     * @param uri [Uri] to an image resource
     * @return a matching [Bitmap] or nil if the file could not be processed
     * @throws FileNotFoundException if the Uri does not reference a valid file
     * @throws IOException if an error occurs while reading the file
     */
    @Throws(FileNotFoundException::class, IOException::class)
    private fun Context.bitmapForUri(uri: Uri): Bitmap? =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            @Suppress("Deprecation")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } else {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        }
}
