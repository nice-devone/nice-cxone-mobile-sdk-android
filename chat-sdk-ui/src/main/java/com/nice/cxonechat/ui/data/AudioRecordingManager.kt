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

package com.nice.cxonechat.ui.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.util.ErrorGroup.LOW
import com.nice.cxonechat.ui.util.checkPermissions
import com.nice.cxonechat.ui.viewmodel.AudioRecordingViewModel
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scope(ChatActivity::class)
@Scoped
internal class AudioRecordingManager(
    private val valueStorage: ValueStorage,
    @InjectedParam internal val audioViewModel: AudioRecordingViewModel,
    @InjectedParam internal val chatStateViewModel: ChatStateViewModel
) {
    private val requiredPermissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @SuppressLint("MissingPermission")
    suspend fun triggerRecording(
        activity: Activity,
        audioRequestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    ): Boolean {
        val granted = activity.checkPermissions(
            valueStorage = valueStorage,
            permissions = requiredPermissions.toSet(),
            rationale = R.string.recording_audio_permission_rationale,
            onAcceptPermissionRequest = { perms -> audioRequestPermissionLauncher.launch(perms) }
        ).also(audioViewModel::setRecordingPermissionGranted)

        if (!granted) {
            return false
        }
        val result = if (audioViewModel.recordingFlow.value) {
            audioViewModel.stopRecording()
        } else {
            audioViewModel.startRecording().isSuccess
        }
        return result
    }

    suspend fun dismissRecording(
        activity: Activity,
        audioRequestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    ) {
        val granted = activity.checkPermissions(
            valueStorage = valueStorage,
            permissions = requiredPermissions.toSet(),
            rationale = R.string.recording_audio_permission_rationale,
            onAcceptPermissionRequest = { perms -> audioRequestPermissionLauncher.launch(perms) }
        ).also(audioViewModel::setRecordingPermissionGranted)

        if (!granted) return

        audioViewModel.deleteLastRecording {
            chatStateViewModel.showError(LOW, activity.getString(R.string.record_audio_failed_cleanup))
        }
    }
}
