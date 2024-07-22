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

package com.nice.cxonechat.ui.composable.conversation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.ui.composable.generic.AudioPlayer
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.TopBar

/**
 * Present an audio player pop dialog.
 *
 * @param url URL of audio to play.
 * @param title Title to display
 * @param onCancel Action when user has cancelled the dialog via back button or a tap outside the dialog.
 */
@Composable
fun AudioPlayerDialog(
    url: String,
    title: String?,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Column {
            title?.let {
                ChatTheme.TopBar(title = it)
            }
            AudioPlayer(uri = Uri.parse(url))
        }
    }
}

@Preview
@Composable
private fun PreviewTitlelessAudioPlayer() {
    ChatTheme {
        AudioPlayerDialog(url = "https://some.url", null) {}
    }
}

@Preview
@Composable
private fun PreviewTitledAudioPlayer() {
    ChatTheme {
        AudioPlayerDialog(url = "https://some.url", "Title") {}
    }
}
