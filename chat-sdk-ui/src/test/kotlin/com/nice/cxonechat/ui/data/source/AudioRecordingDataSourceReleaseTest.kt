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

import android.app.Application
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.log.LoggerNoop
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowMediaRecorder
import java.io.File

/**
 * [AudioRecordingDataSource.release] with a real session: during a recording it must release the recorder and close the
 * file descriptor, and after a normal stop it must not stop the recorder again, which throws on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [AudioRecordingDataSourceReleaseTest.CountingMediaRecorder::class])
internal class AudioRecordingDataSourceReleaseTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(FakeMediaStore::class.java, MediaStore.AUTHORITY)
        CountingMediaRecorder.stopCalls = 0
        CountingMediaRecorder.releaseCalls = 0
        FakeMediaStore.descriptorCloses = 0
    }

    @Test
    fun `release after a finished recording does not stop the recorder again`() = runTest {
        val dataSource = AudioRecordingDataSource(application, LoggerNoop)
        assertTrue(dataSource.startRecording().isSuccess)
        dataSource.stopRecording()

        dataSource.release()

        assertEquals(1, CountingMediaRecorder.stopCalls)
    }

    @Test
    fun `release during a recording releases the recorder and closes its file descriptor`() = runTest {
        val dataSource = AudioRecordingDataSource(application, LoggerNoop)
        assertTrue(dataSource.startRecording().isSuccess)

        dataSource.release()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, CountingMediaRecorder.releaseCalls)
        assertEquals(1, FakeMediaStore.descriptorCloses)
    }

    /** Counts [MediaRecorder.stop] calls, which on a device throw when the recorder is already stopped, and releases. */
    @Implements(MediaRecorder::class)
    internal class CountingMediaRecorder : ShadowMediaRecorder() {
        @Implementation
        override fun stop() {
            stopCalls++
            super.stop()
        }

        @Implementation
        override fun release() {
            releaseCalls++
            super.release()
        }

        companion object {
            var stopCalls = 0
            var releaseCalls = 0
        }
    }

    /** Minimal MediaStore stand-in: inserts succeed, every row is backed by one temporary file, and closes are counted. */
    internal class FakeMediaStore : ContentProvider() {
        private val file by lazy { File.createTempFile("recording", ".amr") }

        override fun onCreate() = true

        override fun insert(uri: Uri, values: ContentValues?): Uri = ContentUris.withAppendedId(uri, 1)

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode), Handler(Looper.getMainLooper())) {
                descriptorCloses++
            }

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 1

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 1

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String = AudioRecordingDataSource.MIME_TYPE

        companion object {
            var descriptorCloses = 0
        }
    }
}
