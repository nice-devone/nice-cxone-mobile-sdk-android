/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The descriptor [AttachmentDataSource] hands out, and who is left holding it when the caller goes away.
 *
 * The content resolver is the one thing faked here: it is the boundary this class opens files through, and faking it
 * is what makes "the open finishes after the caller is cancelled" a latch rather than a race. Everything under test —
 * the cancellation handler, the descriptor it closes — is the real thing.
 *
 * Of the two places the class closes a descriptor it cannot hand over, these pin the resume handler's. The `catch` in
 * `getFileDescriptor` covers a different window — the value delivered, then cancellation — and no test drives it:
 * that window holds no suspension point, so nothing outside the class can land a cancellation inside it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AttachmentDataSourceTest {

    @Test
    fun `local descriptor is handed to the caller still open`() {
        val file = documentFile()
        val resolver = openingResolver(file)
        val dataSource = AttachmentDataSource(resolverContext(resolver), OkHttpClient())

        val descriptor = runBlocking { dataSource.getFileDescriptor(DOCUMENT_URI, null) }.getOrThrow()

        assertTrue("The caller was handed a descriptor which is already closed.", descriptor.fileDescriptor.valid())
        descriptor.close()
    }

    @Test
    fun `local descriptor opened after the caller is cancelled is closed by the resume handler`() {
        val file = documentFile()
        val reachedOpen = CountDownLatch(1)
        val finishOpen = CountDownLatch(1)
        val opened = AtomicReference<ParcelFileDescriptor?>()
        val resolver = mockk<ContentResolver>()
        every { resolver.openFileDescriptor(any(), READ_MODE, any<CancellationSignal>()) } answers {
            reachedOpen.countDown()
            finishOpen.await(SETTLE_SECONDS, TimeUnit.SECONDS)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).also(opened::set)
        }
        val dataSource = AttachmentDataSource(resolverContext(resolver), OkHttpClient())

        runBlocking {
            val request = launch(Dispatchers.IO) { dataSource.getFileDescriptor(DOCUMENT_URI, null) }
            assertTrue("The open was never reached.", reachedOpen.await(SETTLE_SECONDS, TimeUnit.SECONDS))
            // Cancelled while the open is in flight, so it is a descriptor with nowhere to go that the open returns:
            // the resumed value is discarded, leaving the cancellation handler as the only thing that can see it.
            request.cancel()
            finishOpen.countDown()
            request.join()
        }

        val descriptor = requireNotNull(opened.get()) { "The open never returned a descriptor to account for." }
        assertFalse(
            "The descriptor opened as the caller was cancelled was dropped instead of closed.",
            descriptor.fileDescriptor.valid(),
        )
    }

    private fun documentFile(): File = File.createTempFile("attachment", ".pdf").apply {
        writeText("%PDF-1.4")
        deleteOnExit()
    }

    private fun openingResolver(file: File): ContentResolver = mockk<ContentResolver>().apply {
        every { openFileDescriptor(any(), READ_MODE, any<CancellationSignal>()) } answers {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    private fun resolverContext(resolver: ContentResolver): Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getContentResolver(): ContentResolver = resolver
        }

    private companion object {
        const val DOCUMENT_URI = "content://com.nice.cxonechat.test.attachments/document.pdf"

        const val READ_MODE = "r"

        /** Generous, because it is only ever waited out when the code under test has stopped making progress. */
        const val SETTLE_SECONDS = 10L
    }
}
