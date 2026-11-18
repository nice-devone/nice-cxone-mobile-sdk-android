/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class AttachmentDataSourceCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val requestedUrls = CopyOnWriteArrayList<String>()
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val url = chain.request().url.toString()
            requestedUrls += url
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(DOCUMENTS.getValue(url).toResponseBody("application/pdf".toMediaType()))
                .build()
        }
        .build()
    private val logLines = CopyOnWriteArrayList<String>()
    private val logger = object : Logger {
        override fun log(level: Level, message: String, throwable: Throwable?) {
            logLines += message
        }
    }
    private val dataSource = AttachmentDataSource(context, client, logger)

    @After
    fun clearCache() = runTest { dataSource.clearCache() }

    @Test
    fun `two attachments sharing a file name each read their own document`() = runTest {
        assertEquals(FIRST_DOCUMENT, dataSource.read(FIRST_URL, SHARED_NAME))
        assertEquals(SECOND_DOCUMENT, dataSource.read(SECOND_URL, SHARED_NAME))
    }

    @Test
    fun `a second read of one attachment is served from the cache`() = runTest {
        dataSource.read(FIRST_URL, SHARED_NAME)
        dataSource.read(FIRST_URL, SHARED_NAME)

        assertEquals(listOf(FIRST_URL), requestedUrls)
    }

    @Test
    fun `a re-signed url of one attachment is served from the cache`() = runTest {
        dataSource.read(FIRST_URL, SHARED_NAME)

        assertEquals(FIRST_DOCUMENT, dataSource.read(FIRST_URL_RESIGNED, SHARED_NAME))
        assertEquals(listOf(FIRST_URL), requestedUrls)
    }

    @Test
    fun `a file name with path segments stays inside the cache folder`() = runTest {
        assertEquals(FIRST_DOCUMENT, dataSource.read(FIRST_URL, "../../escape.pdf"))
        assertEquals(SECOND_DOCUMENT, dataSource.read(SECOND_URL, "a.b/c"))

        assertFalse(File(context.cacheDir.parentFile, "escape.pdf").exists())
        assertEquals(2, File(context.cacheDir, "remote_files").listFiles().orEmpty().size)
    }

    @Test
    fun `an attachment without a file name is still read`() = runTest {
        assertEquals(FIRST_DOCUMENT, dataSource.read(FIRST_URL, null))
    }

    @Test
    fun `a moved seed is read without a download`() = runTest {
        val seed = checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT)))

        dataSource.adoptSeed(seed, FIRST_URL, SHARED_NAME)

        assertEquals(LOCAL_DOCUMENT, dataSource.read(FIRST_URL_RESIGNED, SHARED_NAME))
        assertEquals(emptyList<String>(), requestedUrls)
        assertFalse(seed.exists())
    }

    @Test
    fun `a seed is deleted when the attachment is already cached`() = runTest {
        dataSource.read(FIRST_URL, SHARED_NAME)
        val seed = checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT)))

        dataSource.adoptSeed(seed, FIRST_URL_RESIGNED, SHARED_NAME)

        assertEquals(FIRST_DOCUMENT, dataSource.read(FIRST_URL, SHARED_NAME))
        assertFalse(seed.exists())
        assertEquals(1, File(context.cacheDir, "remote_files").listFiles().orEmpty().size)
    }

    @Test
    fun `a discarded seed leaves nothing behind`() = runTest {
        val seed = checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT)))

        dataSource.discardSeed(seed)

        assertTrue(File(context.cacheDir, "remote_files").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `an unreadable local file gives no seed`() = runTest {
        assertEquals(null, dataSource.seed(Uri.fromFile(File(context.filesDir, "missing.pdf")).toString()))
        assertTrue(File(context.cacheDir, "remote_files").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `a miss then a hit are both logged`() = runTest {
        dataSource.read(FIRST_URL, SHARED_NAME)
        dataSource.read(FIRST_URL, SHARED_NAME)

        assertEquals(1, logLines.count { "Cache miss" in it && "wrote ${FIRST_DOCUMENT.length} bytes" in it })
        assertEquals(1, logLines.count { "Cache hit" in it })
    }

    @Test
    fun `a moved and a deleted seed are both logged`() = runTest {
        dataSource.adoptSeed(checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT))), FIRST_URL, SHARED_NAME)
        dataSource.adoptSeed(checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT))), FIRST_URL, SHARED_NAME)

        assertEquals(1, logLines.count { "Seed moved to" in it })
        assertEquals(1, logLines.count { "Seed deleted" in it })
    }

    @Test
    fun `the log never carries the signed url or the local uri`() = runTest {
        dataSource.adoptSeed(checkNotNull(dataSource.seed(localFile(LOCAL_DOCUMENT))), SECOND_URL, SHARED_NAME)
        dataSource.read(FIRST_URL, SHARED_NAME)
        dataSource.read(FIRST_URL, SHARED_NAME)
        dataSource.clearCache()

        assertTrue(logLines.isNotEmpty())
        assertTrue(logLines.none { "Token" in it || "files.example.test" in it || "picked.pdf" in it })
    }

    @Test
    fun `clearing the cache logs how many files it removed`() = runTest {
        dataSource.read(FIRST_URL, SHARED_NAME)
        dataSource.read(SECOND_URL, SHARED_NAME)

        dataSource.clearCache()

        assertTrue(logLines.any { "Removed 2 cached files" in it })
    }

    private fun localFile(content: String): String =
        Uri.fromFile(File(context.filesDir, "picked.pdf").apply { writeText(content) }).toString()

    private suspend fun AttachmentDataSource.read(url: String, fileName: String?): String =
        ParcelFileDescriptor.AutoCloseInputStream(getFileDescriptor(url, fileName).getOrThrow())
            .use { it.readBytes().decodeToString() }

    private companion object {
        const val SHARED_NAME = "sample_cats_landscape.pdf"
        const val FIRST_URL = "https://files.example.test/attachments/ffd6b952/download?downloadAttachmentToken=first"
        const val FIRST_URL_RESIGNED =
            "https://files.example.test/attachments/ffd6b952/download?downloadAttachmentToken=resigned"
        const val SECOND_URL = "https://files.example.test/attachments/2248bcde/download?downloadAttachmentToken=second"
        const val FIRST_DOCUMENT = "first document"
        const val SECOND_DOCUMENT = "second document"
        const val LOCAL_DOCUMENT = "local document"
        val DOCUMENTS = mapOf(
            FIRST_URL to FIRST_DOCUMENT,
            FIRST_URL_RESIGNED to FIRST_DOCUMENT,
            SECOND_URL to SECOND_DOCUMENT,
        )
    }
}
