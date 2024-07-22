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

import android.util.Base64
import android.webkit.MimeTypeMap
import com.nice.cxonechat.message.ContentDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import kotlin.io.encoding.Base64.Default.encode
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.assertEquals

class AttachmentUploadModelTest {
    val mimeTypeMap: MimeTypeMap = mockk<MimeTypeMap> {
        every { getExtensionFromMimeType(mimeType) } returns extension
    }.also {
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns it
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Before
    fun setup() {
        // since android.* classes aren't implemented for unit tests, mock out Base64 conversion
        // to just return a fixed string
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers { encode(arg<ByteArray>(0)) }
    }

    @Test
    fun constructorAppliesExtension() {
        val model = AttachmentUploadModel(content, mimeType, filename)

        assertEquals(content, model.content)
        assertEquals(mimeType, model.mimeType)
        assertEquals("filename.$extension", model.fileName)
    }

    @Test
    fun constructorKeepsExtension() {
        val model = AttachmentUploadModel(content, mimeType, "filename.txt")

        assertEquals(content, model.content)
        assertEquals(mimeType, model.mimeType)
        assertEquals("filename.txt", model.fileName)
    }

    @Test
    fun contentDescriptorConstructorAppliesDefaultFilename() {
        val contentDescriptor = ContentDescriptor(bytes, mimeType, filename, null)
        val model = AttachmentUploadModel(contentDescriptor)

        assertEquals(content, model.content)
        assertEquals(mimeType, model.mimeType)
        assertEquals("filename.$extension", model.fileName)
    }

    @Test
    fun contentDescriptorConstructorAppliesKeepsFriendlyName() {
        val contentDescriptor = ContentDescriptor(bytes, mimeType, filename, "friendly")
        val model = AttachmentUploadModel(contentDescriptor)

        assertEquals(content, model.content)
        assertEquals(mimeType, model.mimeType)
        assertEquals("friendly.$extension", model.fileName)
    }

    companion object {
        const val filename = "filename"
        const val mimeType = "application/wtf"
        const val extension = "wtf"
        val bytes: ByteArray by lazy { Random.nextBytes(32) }

        @OptIn(ExperimentalEncodingApi::class)
        val content: String by lazy { encode(bytes) }
    }
}
