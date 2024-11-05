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

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

/**
 * A [CardDialog] for video which can be replaced with [FullscreenView] if the user,
 * toggles the fullscreen button.
 * The dialog is dismissed if the user click outside the view or taps the back button.
 * The fullscreen view is dismissed back to the dialog.
 *
 * @param uri The [Uri] of the video to be played.
 * @param title An optional title for the [CardDialog].
 * @param onDismiss An action which will be triggered if the view is dismissed.
 */
@Composable
internal fun VideoViewerDialogCard(
    uri: String,
    title: String?,
    onDismiss: () -> Unit,
) {
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    val parsedUri by remember {
        derivedStateOf {
            runCatching { Uri.parse(uri) }.getOrNull()
        }
    }
    val content: @Composable () -> Unit = {
        if (parsedUri != null) {
            VideoPlayer(uri = parsedUri, onFullScreenClickListener = { isFullScreen = it })
        } else {
            Text(text = stringResource(androidx.media3.exoplayer.R.string.exo_download_failed))
        }
    }
    if (isFullScreen) {
        FullscreenView(
            title = title,
            onExitFullScreen = { isFullScreen = false }
        ) {
            content()
        }
    } else {
        CardDialog(
            title = title,
            onDismiss = onDismiss
        ) {
            content()
        }
    }
}
