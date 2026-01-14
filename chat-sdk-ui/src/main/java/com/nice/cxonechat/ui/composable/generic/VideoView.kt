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

package com.nice.cxonechat.ui.composable.generic

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ShareButton

/**
 * A FullScreen view for video which can be replaced with [FullscreenView] if the user,
 * toggles the fullscreen button.
 * The dialog is dismissed if the user click outside the view or taps the back button.
 * The fullscreen view is dismissed back to the dialog.
 *
 * @param uri The [Uri] of the video to be played.
 * @param title An optional title for the Activity while the composable is displayed.
 * @param onDismiss An action which will be triggered if the view is dismissed.
 * @param onShare An action which will be triggered if the share button is clicked.
 */
@Composable
internal fun VideoView(
    uri: String,
    title: String?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val parsedUri by remember {
        derivedStateOf {
            runCatching { uri.toUri() }.getOrNull()
        }
    }
    val content: @Composable () -> Unit = {
        if (parsedUri != null) {
            VideoPlayer(
                uri = parsedUri,
                modifier = Modifier.testTag("video_player"),
            )
        } else {
            Text(
                modifier = Modifier.testTag("download_failed_text"),
                text = stringResource(androidx.media3.exoplayer.R.string.exo_download_failed)
            )
        }
    }
    FullscreenView(
        title = title,
        onExitFullScreen = onDismiss,
        modifier = Modifier.testTag("video_view")
    ) {
        Column(horizontalAlignment = CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.medium)
            ) {
                BackButton(onDismiss)
                ShareButton(title, onShare)
            }
            content()
        }
    }
}

@Preview
@Composable
private fun VideoViewerPreview() {
    ChatTheme {
        Surface {
            VideoView(
                uri = PreviewAttachments.movie.url,
                title = PreviewAttachments.movie.friendlyName,
                onDismiss = { },
                onShare = { },
            )
        }
    }
}
