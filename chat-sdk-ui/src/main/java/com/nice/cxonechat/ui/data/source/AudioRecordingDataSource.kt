/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.util.belongsToCurrentApplication
import com.nice.cxonechat.ui.util.isExported
import com.nice.cxonechat.ui.util.isValidFile
import com.nice.cxonechat.ui.util.wasGrantedPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import java.io.File
import java.io.FileDescriptor
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A data source for managing audio recording functionality.
 * Handles starting, stopping, and deleting audio recordings, as well as managing file descriptors and URIs through the
 * [MediaStore].
 *
 * @param context The application context.
 * @param logger A parent [Logger] scope/instance for logging of events and errors.
 */
@Factory
internal class AudioRecordingDataSource(
    private val context: Context,
    @Named(UiModule.LOGGER_NAME) logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {

    private var session: AudioRecordSession? = null
        set(value) {
            field?.runCatching { close() }
            field = value
        }

    private fun newFilename(): String = FILE_TEMPLATE.format(SimpleDateFormat.getDateTimeInstance().format(Date()))

    /**
     * Starts a new audio recording session.
     * Requires RECORD_AUDIO and WRITE_EXTERNAL_STORAGE permissions.
     *
     * @return A [Result] containing the URI of the recorded audio file. The [Result] will be successful iff the
     * file for recording was created successfully and the recording was started without an exception.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ]
    )
    suspend fun startRecording(): Result<Uri> = scope("startRecording") {
        withContext(Dispatchers.IO) {
            runCatching {
                val filename = newFilename()
                val (uri, descriptor) = getFileDescriptorAndSetUri(context, filename)
                val newRecorder: MediaRecorder = prepareRecording(context, descriptor.fileDescriptor)
                session = AudioRecordSession(
                    releasableRecorder = newRecorder,
                    releasableFileDescriptor = descriptor,
                    uri = uri,
                    logger = this@AudioRecordingDataSource,
                )
                newRecorder.start()
                uri
            }.onFailure {
                warning("Failed to start recording", it)
                session = null
            }
        }
    }

    /**
     * Stops the current audio recording session.
     *
     * @return The URI of the recorded audio file, or null if no session exists or there was an error while stopping the
     * recording.
     */
    suspend fun stopRecording(): Uri? {
        stopRecorder()
        val recordingUri = session?.uri ?: return null
        return runCatching<Uri?> {
            context.clearPendingStatus(recordingUri)
            recordingUri
        }.getOrNull()
    }

    /**
     * Deletes the current audio recording, no-op if there is no recording.
     */
    suspend fun deleteRecording() {
        session?.let { context.deleteRecording(it, this) }
    }

    private suspend fun getFileDescriptorAndSetUri(context: Context, file: String): Pair<Uri, ParcelFileDescriptor> =
        withContext(Dispatchers.Default) {
            val uri = context.preparePendingRecording(file)
            require(uri != null && uri != Uri.EMPTY) { "Unable to get uri for $file" }
            val parcelFileDescriptor = context.applicationContext.contentResolver.openFileDescriptor(uri, "rw")
            uri to requireNotNull(parcelFileDescriptor) { "Unable to open file descriptor for $uri" }
        }

    private suspend fun prepareRecording(context: Context, parcelFileDescriptor: FileDescriptor): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) isPrivacySensitive = true
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setOutputFile(parcelFileDescriptor)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            withContext(Dispatchers.IO) { prepare() }
        }
    }

    private fun stopRecorder() = scope("stopRecorder") {
        session?.stop()
    }

    companion object {
        private const val TAG = "AudioRecordingDataSource"

        private const val FILE_TEMPLATE = "%s voice message.amr"

        internal const val MIME_TYPE = "audio/amr"
        private suspend fun Context.preparePendingRecording(fileName: String): Uri? = withContext(Dispatchers.IO) {
            val resolver = applicationContext.contentResolver
            val recordingCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val recordingDetails = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) put(MediaStore.Audio.Media.IS_RECORDING, 1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Audio.Media.DATA, File(getExternalDir(), fileName).absolutePath)
                }
            }
            resolver.insert(recordingCollection, recordingDetails)
        }

        private suspend fun Context.clearPendingStatus(recordingUri: Uri) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                withContext(Dispatchers.IO) {
                    val recordingDetails = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    applicationContext.contentResolver.update(recordingUri, recordingDetails, null, null)
                }
            }
        }

        private suspend fun Context.deleteRecording(recordingSession: AudioRecordSession, scope: LoggerScope) {
            val recordingUri = recordingSession.uri
            scope.scope("deleteRecording") {
                withContext(Dispatchers.IO) {
                    if (canDeleteUri(applicationContext, recordingSession)) {
                        runCatching(recordingSession::close).onFailure {
                            scope.warning("Failed to close recording session for $recordingUri", it)
                        }
                        applicationContext.contentResolver.delete(recordingUri, null, null)
                    } else {
                        scope.warning("Cannot delete recording $recordingUri, it does not belong to this application or is not exported.")
                    }
                }
            }
        }

        private fun canDeleteUri(context: Context, recordingSession: AudioRecordSession): Boolean = runCatching {
            val (recordingUri, pfd) = recordingSession.uri to recordingSession.fileDescriptor
            recordingUri.belongsToCurrentApplication(context) ||
                    recordingUri.isExported(context) &&
                    recordingUri.wasGrantedPermission(context) &&
                    recordingUri.isValidFile(pfd)
        }.getOrDefault(false)

        private fun Context.getExternalDir(): File {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ?: noBackupFilesDir
            if (!directory.exists()) {
                directory.mkdir()
            }
            return directory
        }
    }
}

private class AudioRecordSession(
    releasableRecorder: MediaRecorder,
    releasableFileDescriptor: ParcelFileDescriptor,
    val uri: Uri,
    val logger: Logger,
) : LoggerScope by LoggerScope("AudioRecordSession", logger), AutoCloseable {

    var recorder: MediaRecorder? = releasableRecorder
        private set(value) {
            field?.runCatching { release() }
            field = value
        }

    var fileDescriptor: ParcelFileDescriptor? = releasableFileDescriptor
        private set(value) {
            field?.runCatching { close() }
            field = value
        }

    fun stop() = scope("release") {
        runCatching {
            recorder?.stop()
        }.onFailure {
            error("Failed to release resources for $uri", it)
        }
    }

    override fun close() {
        fileDescriptor = null
        recorder = null
    }
}
