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

package com.nice.cxonechat.ui.composable.generic

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.nice.cxonechat.ui.R.string

/**
 * Show toast informing user that audio record attempt has failed.
 * @param isRecording Flag for proper choice of information text. The flag indicates the state of the UI when
 * the audio record attempt was made.
 * If `true` the recording was succesfully started, but it failed to stop the recording properly;
 * otherwise the attempt has failed when the UI has attempted to start the recoding.
 */
internal fun Context.toastAudioRecordToggleFailure(isRecording: Boolean) {
    if (isRecording) {
        Toast.makeText(this, string.recording_audio_failed, LENGTH_LONG).show()
    } else {
        Toast.makeText(this, string.recording_audio_failed_to_start, LENGTH_LONG).show()
    }
}
