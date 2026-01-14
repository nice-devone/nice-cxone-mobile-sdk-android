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

package com.nice.cxonechat.ui.viewmodel

import android.Manifest
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import com.nice.cxonechat.ui.data.source.AudioRecordingDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * ViewModel, which is responsible for providing state-based audio recording functionality.
 */
@KoinViewModel
internal class AudioRecordingViewModel(
    lazyAllowedFileTypeSource: Lazy<AllowedFileTypeSource>,
    private val audioRecordingDataSource: AudioRecordingDataSource,
) : ViewModel() {
    private val fileRestrictions: AllowedFileTypeSource by lazyAllowedFileTypeSource

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

    private val internalRecordDurationFlow = MutableStateFlow(Duration.ZERO)

    /** [StateFlow] which indicates the duration of the current recording. */
    val recordDurationFlow: StateFlow<Duration> = internalRecordDurationFlow.asStateFlow()

    /** Job for updating the duration of the recording. */
    private var durationJob: Job? = null

    /**
     * [StateFlow] which indicates if the recording permission is granted, initial value is `true` to allow requesting
     * of the permissions.
     */
    private val recordingPermissionGrantedFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    // Determines if audio attachments are allowed based on MIME type restrictions.
    private val isAudioAttachmentAllowed: Boolean by lazy {
        fileRestrictions.allowedMimeTypes.any {
            it.mimeType == "audio/*" || it.mimeType == AudioRecordingDataSource.MIME_TYPE
        }
    }

    /**
     * Public [Flow] indicating if recording is allowed based on permissions and file type restrictions.
     */
    val isRecordingAllowed: Flow<Boolean> by lazy {
        recordingPermissionGrantedFlow
            .map { isAudioAttachmentAllowed && it }
            .conflate()
    }

    /**
     * Sets the recording permission granted state.
     */
    fun setRecordingPermissionGranted(granted: Boolean) {
        recordingPermissionGrantedFlow.value = granted
    }

    /**
     * Starts/restarts audio recording.
     *
     * Requires the following permissions:
     * - [Manifest.permission.RECORD_AUDIO]
     * - [Manifest.permission.WRITE_EXTERNAL_STORAGE] (only on Android Q and lower)
     *
     * @return [Result] indicating if the recording was successfully started.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ]
    )
    suspend fun startRecording(): Result<Unit> {
        return audioRecordingDataSource.startRecording().map {
            internalRecordingFlow.value = true
            startDurationJob()
        }
    }

    /**
     * Stops current audio recording in progress and releases resources allocated for the recording.
     *
     * Permission [Manifest.permission.WRITE_EXTERNAL_STORAGE] is required only on [android.os.Build.VERSION_CODES.Q] and lower.
     *
     * @return `true` if the recording was successfully stopped and the recording is available, `false` otherwise.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ],
        conditional = true
    )
    suspend fun stopRecording(): Boolean {
        val recordingUri = audioRecordingDataSource.stopRecording() ?: return false
        internalRecordingFlow.value = false
        durationJob?.cancel()
        durationJob = null
        internalRecordedUriFlow.value = recordingUri
        return true
    }

    /**
     * Deletes the last audio recording.
     *
     * Requires the following permissions:
     * - [Manifest.permission.WRITE_EXTERNAL_STORAGE] (only on Android Q and lower)
     *
     * @param onFailure Callback invoked if the deletion fails.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ],
        conditional = true
    )
    fun deleteLastRecording(onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            runCatching {
                audioRecordingDataSource.deleteRecording()
                internalRecordedUriFlow.value = Uri.EMPTY
            }.onFailure(onFailure)
        }
    }

    override fun onCleared() {
        viewModelScope.launch { audioRecordingDataSource.stopRecording() }
        super.onCleared()
    }

    /**
     * Starts a coroutine job to update the recording duration every second.
     */
    private fun startDurationJob() {
        durationJob = viewModelScope.launch {
            internalRecordDurationFlow.value = Duration.ZERO
            while (isActive) {
                delay(1000L)
                internalRecordDurationFlow.value += 1L.toDuration(DurationUnit.SECONDS)
            }
        }
    }
}
