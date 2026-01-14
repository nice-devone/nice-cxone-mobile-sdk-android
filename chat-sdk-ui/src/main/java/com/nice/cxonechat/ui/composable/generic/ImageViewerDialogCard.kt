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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ShareButton

/**
 * A FullscreenView which will display a [ZoomableImage] with option to share image via button.
 * The dialog is dismissed if the user click outside the view or taps the back button or clicks the back button icon.
 *
 * @param image The model for [ZoomableImage].
 * @param title An optional title for the Activity while the composable is displayed.
 * @param onDismiss An action triggered when the dialog is dismissed.
 * @param onShare An action triggered when the share button is clicked.
 */
@Composable
internal fun ImageViewerDialogCard(
    image: Any?,
    title: String?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    FullscreenView(
        title = title,
        onExitFullScreen = onDismiss,
        modifier = Modifier.testTag("image_view")
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.medium)
            ) {
                BackButton(onDismiss)
                ShareButton(title, onShare)
            }
            ZoomableImage(
                image = image,
                contentDescription = title,
                modifier = Modifier
                    .testTag("zoomable_image")
                    .fillMaxSize()
            )
        }
    }
}

@Composable
@Preview(showBackground = false, showSystemUi = true)
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
            },
            onShare = {
                Toast.makeText(context, "Shared", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
