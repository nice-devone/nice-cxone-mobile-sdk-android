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

package com.nice.cxonechat.ui.main

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.ui.PushListenerService
import com.nice.cxonechat.ui.UiModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get
import java.io.File
import java.io.FileDescriptor
import java.text.SimpleDateFormat
import java.util.Date

/**
 * ViewModel, which is responsible for providing state-based audio recording functionality.
 */
@KoinViewModel
internal class AudioRecordingViewModel : ViewModel() {

    private val logger by lazy { LoggerScope(PushListenerService.TAG, get(Logger::class.java, named(UiModule.loggerName))) }

    private val internalRecordingUriFlow: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    private var filename: String? = null
    private var recorder: MediaRecorder? = null
        set(value) {
            field?.runCatching { release() }
            field = value
        }
    private var fileDescriptor: ParcelFileDescriptor? = null
        set(value) {
            field?.close()
            field = value
        }
    private val internalRecordingFlow = MutableStateFlow(false)

    /**
     * [StateFlow] which indicates if the audio recording is in progress.
     */
    val recordingFlow: StateFlow<Boolean> = internalRecordingFlow.asStateFlow()

    private val internalRecordedUriFlow: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)

    /**
     * [StateFlow] which points to last file used for audio record, after the recording was successfully finished.
     */
    val recordedUriFlow: StateFlow<Uri> = internalRecordedUriFlow.asStateFlow()

    private fun newFilename(): String = "${SimpleDateFormat.getDateTimeInstance().format(Date())} voice message.amr"

    /**
     * Starts/restarts audio recording.
     *
     * Permission [Manifest.permission.WRITE_EXTERNAL_STORAGE] is required only on [Build.VERSION_CODES.Q] and lower.
     *
     * @return [Result] which indicates if the recording was successfully started.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ]
    )
    suspend fun startRecording(context: Context): Result<Unit> = logger.scope("startRecording") {
        runCatching {
            val descriptor = getFileDescriptorAndSetUri(context)
            val newRecorder = prepareRecording(context, descriptor.fileDescriptor)
            fileDescriptor = descriptor
            recorder = newRecorder
            newRecorder.start()
            internalRecordingFlow.value = true
        }.onFailure {
            error("startRecording: ", it)
        }
    }

    private suspend fun getFileDescriptorAndSetUri(context: Context): ParcelFileDescriptor = withContext(Dispatchers.IO) {
        val file = filename ?: newFilename()
        filename = file
        val uri = context.preparePendingRecording(file)
        requireNotNull(uri) { "Unable to get uri for $file" }
        internalRecordingUriFlow.value = uri
        val parcelFileDescriptor = context.applicationContext.contentResolver.openFileDescriptor(uri, "rw")
        requireNotNull(parcelFileDescriptor) { "Unable to open file descriptor for $uri" }
    }

    /**
     * Deletes last audio recording.
     *
     * Permission [Manifest.permission.WRITE_EXTERNAL_STORAGE] is required only on [Build.VERSION_CODES.Q] and lower.
     */
    @RequiresPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun deleteLastRecording(context: Context, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            internalRecordedUriFlow.value = Uri.EMPTY
            filename = null
            runCatching {
                val uri = internalRecordingUriFlow.value.takeIf { it != Uri.EMPTY }
                requireNotNull(uri) { "Last recording reference is not valid" }
                context.deleteRecording(uri)
                internalRecordingUriFlow.value = Uri.EMPTY
                internalRecordedUriFlow.value = Uri.EMPTY
            }.onFailure(onFailure)
        }
    }

    private fun prepareRecording(context: Context, parcelFileDescriptor: FileDescriptor): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") // Deprecated from API 31, not before
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setOutputFile(parcelFileDescriptor)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
        }
    }

    /**
     * Stops current audio recording in progress and releases resources allocated for the recording.
     *
     * Permission [Manifest.permission.WRITE_EXTERNAL_STORAGE] is required only on [Build.VERSION_CODES.Q] and lower.
     *
     * @return [Uri] of the recorded audio file, if it is available. Missing uri indicates failure in the recording process.
     */
    suspend fun stopRecording(context: Context): Boolean {
        stopRecorder()
        recorder = null
        internalRecordingFlow.value = false
        filename = null
        val recordingUri = internalRecordingUriFlow.value.takeIf { it != Uri.EMPTY } ?: return false
        context.clearPendingStatus(recordingUri)
        internalRecordedUriFlow.value = recordingUri
        return true
    }

    /**
     * Stops&releases the audio recorder and also logs potential exceptions.
     */
    private fun stopRecorder() = logger.scope("stopRecorder") {
        val mediaRecorder = recorder ?: return@scope
        mediaRecorder.runCatching {
            stop()
            release()
        }.onFailure {
            error("Failure during stopRecording().", it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder = null
        fileDescriptor = null
    }

    companion object {
        const val TAG = "AudioRecordingViewModel"

        private fun Context.preparePendingRecording(fileName: String): Uri? {
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
                    // For older Android APIs we need to supply the path where the file will be stored
                    put(
                        MediaStore.Audio.Media.DATA,
                        File(getExternalDir(), fileName).absolutePath
                    )
                }
            }
            return resolver.insert(recordingCollection, recordingDetails)
        }

        private fun Context.getExternalDir(): File {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ?: noBackupFilesDir
            if (!directory.exists()) {
                directory.mkdir()
            }
            return directory
        }

        private suspend fun Context.clearPendingStatus(recordingUri: Uri): Uri {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                withContext(Dispatchers.IO) {
                    val recordingDetails = ContentValues()
                    recordingDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    applicationContext.contentResolver.update(recordingUri, recordingDetails, null, null)
                }
            }
            return recordingUri
        }

        private suspend fun Context.deleteRecording(recordingUri: Uri) = withContext(Dispatchers.IO) {
            applicationContext.contentResolver.delete(recordingUri, null, null)
        }
    }
}
