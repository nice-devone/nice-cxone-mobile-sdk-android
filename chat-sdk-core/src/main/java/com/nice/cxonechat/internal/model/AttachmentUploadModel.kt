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

package com.nice.cxonechat.internal.model

import android.annotation.SuppressLint
import android.util.Base64
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.ContentDescriptor.DataSource
import com.nice.cxonechat.util.applyDefaultExtension
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

@Suppress("UseDataClass")
@Serializable
internal class AttachmentUploadModel {
    @SerialName("content")
    val content: String

    @SerialName("mimeType")
    val mimeType: String

    @SerialName("fileName")
    val fileName: String

    /**
     * Default constructor from direct properties.
     *
     * @param content base-64 encoded content to send
     * @param mimeType mime type of the attachment
     * @param fileName "friendly" name of the attachment.  If the name has no extension,
     * then a default extension will be applied based on [mimeType].
     */
    constructor(content: String, mimeType: String, fileName: String) {
        this.content = content
        this.mimeType = mimeType
        this.fileName = fileName.applyDefaultExtension(mimeType)
    }

    /**
     * Convenience constructor that gets all necessary fields from the passed
     * [ContentDescriptor].
     *
     * @param upload [ContentDescriptor] containing attachment details
     * @throws IOException if the attachment cannot be read
     */
    @Throws(IOException::class)
    constructor(upload: ContentDescriptor): this(
        content = upload.content.read().base64,
        mimeType = upload.mimeType,
        fileName = upload.friendlyName ?: upload.fileName
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentUploadModel

        if (content != other.content) return false
        if (mimeType != other.mimeType) return false
        return fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }

    override fun toString() = "AttachmentUploadModel(content='$content', mimeType='$mimeType', fileName='$fileName')"

    companion object {
        /**
         * Encode the receiving [ByteArray] as a base-64 encoded string.
         *
         * @receiver [ByteArray] to be encoded
         * @return receiver encoded as a base-64 encoded string
         */
        private val ByteArray.base64: String
            get() = Base64.encodeToString(this, Base64.DEFAULT)

        /**
         * Read the receiving [DataSource] as a ByteArray.
         *
         * @receiver [DataSource] to be encoded
         * @return data from [DataSource] as a base-64 encoded string
         * @throws IOException if the data source can't be read
         */
        @SuppressLint(
            "Recycle" // FP - opened InputStream is closed via the `use` function.
        )
        @Throws(IOException::class)
        private fun DataSource.read() = when (this) {
            is DataSource.Uri ->
                context.contentResolver
                    .openInputStream(uri)
                    ?.use(InputStream::readBytes)
                    ?: throw FileNotFoundException(uri.toString())
            is DataSource.Bytes -> bytes
        }
    }
}
