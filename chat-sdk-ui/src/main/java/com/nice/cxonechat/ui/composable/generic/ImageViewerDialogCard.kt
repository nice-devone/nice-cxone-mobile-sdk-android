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

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * A [CardDialog] which will display a [ZoomableImage] with option to switch to the [FullscreenView] via button.
 * The dialog is dismissed if the user click outside the view or taps the back button.
 * The fullscreen view is dismissed back to the dialog.
 *
 * @param image The model for [ZoomableImage].
 * @param title An optional title for the [CardDialog].
 * @param onDismiss An action triggered when the dialog is dismissed.
 */
@Composable
internal fun ImageViewerDialogCard(
    image: Any?,
    title: String?,
    onDismiss: () -> Unit,
) {
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    val content: @Composable BoxScope.() -> Unit = {
        ZoomableImage(
            image = image,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
        )
    }
    AnimatedContent(
        targetState = isFullScreen,
        label = "fullScreen",
    ) { fullScreen ->
        if (fullScreen) {
            FullscreenView(
                title = title,
                onExitFullScreen = { isFullScreen = false }
            ) {
                FullscreenButtonWrapper(
                    isFullScreen = true,
                    onTriggerFullScreen = { isFullScreen = it },
                    content = content
                )
            }
        } else {
            CardDialog(
                title = title,
                onDismiss = onDismiss
            ) {
                FullscreenButtonWrapper(
                    isFullScreen = false,
                    onTriggerFullScreen = { isFullScreen = it },
                    content = content
                )
            }
        }
    }
}

@Composable
@Preview
private fun PreviewDialog() {
    @Suppress("MaxLineLength")
    val image = """https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/St_Michael%27s_Mount_II5302_x_2982.jpg/1024px-St_Michael%27s_Mount_II5302_x_2982.jpg"""

    ChatTheme {
        val context = LocalContext.current
        ImageViewerDialogCard(
            image = image,
            title = "St Michaels Mount, Marazion in Cornwall UK",
            onDismiss = {
                Toast.makeText(context, "Dismissed", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
@Preview
private fun PreviewCardTitle() {
    ChatTheme {
        Surface {
            CardTitle(title = "St Michaels Mount, Marazion in Cornwall UK")
        }
    }
}
