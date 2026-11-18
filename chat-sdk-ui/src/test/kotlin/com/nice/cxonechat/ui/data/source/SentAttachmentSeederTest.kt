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
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.ui.viewmodel.PreparedAttachment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class SentAttachmentSeederTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val requestedUrls = CopyOnWriteArrayList<String>()
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            requestedUrls += chain.request().url.toString()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(REMOTE_DOCUMENT.toResponseBody(PDF.toMediaType()))
                .build()
        }
        .build()
    private val dataSource = AttachmentDataSource(context, client)
    private val seeder = SentAttachmentSeeder(dataSource)
    private val messageId = UUID.randomUUID()

    @After
    fun clearCache() = runTest { dataSource.clearCache() }

    @Test
    fun `two same-named pdfs in one message each keep their own local file`() = runTest {
        val sent = listOf(picked("first.pdf", "first local"), picked("second.pdf", "second local"))
        seeder.register(messageId, seeder.seed(sent))

        seeder.onEcho(messageId, listOf(echoed(FIRST_URL, SHARED_NAME), echoed(SECOND_URL, SHARED_NAME)))

        assertEquals("first local", dataSource.read(FIRST_URL))
        assertEquals("second local", dataSource.read(SECOND_URL))
        assertEquals(emptyList<String>(), requestedUrls)
    }

    @Test
    fun `an echo with fewer attachments deletes every seed`() = runTest {
        val sent = listOf(picked("first.pdf", "first local"), picked("second.pdf", "second local"))
        seeder.register(messageId, seeder.seed(sent))

        seeder.onEcho(messageId, listOf(echoed(SECOND_URL, SHARED_NAME)))

        assertEquals(REMOTE_DOCUMENT, dataSource.read(SECOND_URL))
        assertEquals(1, cachedFiles().size)
    }

    @Test
    fun `an echo with another name at the same position deletes the seed`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("first.pdf", "first local"))))

        seeder.onEcho(messageId, listOf(echoed(FIRST_URL, "renamed.pdf")))

        assertEquals(REMOTE_DOCUMENT, dataSource.read(FIRST_URL))
    }

    @Test
    fun `an echo of another message changes nothing`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("first.pdf", "first local"))))

        seeder.onEcho(UUID.randomUUID(), listOf(echoed(FIRST_URL, SHARED_NAME)))

        assertEquals(setOf(messageId), seeder.pendingIds)
        assertEquals(1, cachedFiles().size)
    }

    @Test
    fun `received messages adopt only the pending ones`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("first.pdf", "first local"))))
        val echo = mockk<Message> { every { attachments } returns listOf(echoed(FIRST_URL, SHARED_NAME)) }
        val other = mockk<Message>(relaxed = true)

        seeder.adoptEchoes(mapOf(messageId to echo, UUID.randomUUID() to other))

        assertEquals("first local", dataSource.read(FIRST_URL))
        assertTrue(seeder.pendingIds.isEmpty())
    }

    @Test
    fun `a second echo of one message is ignored`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("first.pdf", "first local"))))
        val echo = listOf(echoed(FIRST_URL, SHARED_NAME))

        seeder.onEcho(messageId, echo)
        seeder.onEcho(messageId, echo)

        assertEquals("first local", dataSource.read(FIRST_URL))
        assertTrue(seeder.pendingIds.isEmpty())
    }

    @Test
    fun `a pdf whose mime type differs in case or has parameters is seeded`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("first.pdf", "first local", "Application/PDF; version=1.7"))))

        seeder.onEcho(messageId, listOf(echoed(FIRST_URL, SHARED_NAME)))

        assertEquals("first local", dataSource.read(FIRST_URL))
        assertEquals(emptyList<String>(), requestedUrls)
    }

    @Test
    fun `an echo cancelled while moving seeds still moves every seed`() = runTest {
        val sent = listOf(picked("first.pdf", "first local"), picked("second.pdf", "second local"))
        seeder.register(messageId, seeder.seed(sent))
        val echo = listOf(echoed(FIRST_URL, SHARED_NAME), echoed(SECOND_URL, SHARED_NAME))

        launch(start = CoroutineStart.UNDISPATCHED) { seeder.onEcho(messageId, echo) }.cancelAndJoin()

        assertEquals("first local", dataSource.read(FIRST_URL))
        assertEquals("second local", dataSource.read(SECOND_URL))
        assertEquals(emptyList<String>(), requestedUrls)
    }

    @Test
    fun `a message without pdfs registers nothing`() = runTest {
        seeder.register(messageId, seeder.seed(listOf(picked("photo.jpg", "image bytes", mimeType = "image/jpeg"))))

        assertTrue(seeder.pendingIds.isEmpty())
        assertTrue(cachedFiles().isEmpty())
    }

    private fun picked(localName: String, content: String, mimeType: String = PDF): PreparedAttachment {
        val file = File(context.filesDir, localName).apply { writeText(content) }
        return PreparedAttachment(
            contentDescriptor = ContentDescriptor(
                content = Uri.fromFile(file),
                context = context,
                mimeType = mimeType,
                fileName = "${UUID.randomUUID()}.pdf",
                friendlyName = if (mimeType.startsWith(PDF, ignoreCase = true)) SHARED_NAME else localName,
            ),
            url = Uri.fromFile(file).toString(),
        )
    }

    private fun echoed(url: String, name: String): Attachment = object : Attachment {
        override val url = url
        override val friendlyName = name
        override val mimeType = PDF
    }

    private fun cachedFiles() = File(context.cacheDir, "remote_files").listFiles().orEmpty().toList()

    private suspend fun AttachmentDataSource.read(url: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(getFileDescriptor(url, SHARED_NAME).getOrThrow())
            .use { it.readBytes().decodeToString() }

    private companion object {
        const val PDF = "application/pdf"
        const val SHARED_NAME = "de178946_collision.pdf"
        const val FIRST_URL = "https://files.example.test/attachments/7d4afad2/download?downloadAttachmentToken=a"
        const val SECOND_URL = "https://files.example.test/attachments/4af5c03b/download?downloadAttachmentToken=b"
        const val REMOTE_DOCUMENT = "remote document"
    }
}
