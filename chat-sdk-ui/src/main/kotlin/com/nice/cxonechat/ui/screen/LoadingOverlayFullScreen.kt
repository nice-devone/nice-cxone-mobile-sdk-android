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

package com.nice.cxonechat.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nice.cxonechat.ui.R.integer
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.DefaultColors.overlayBackground
import kotlinx.coroutines.delay

/**
 * Composable function that displays a full-screen loading overlay with a close button after a delay.
 *
 * @param modifier Modifier to be applied to the overlay.
 * @param closeButtonText Text to display on the close button.
 * @param onClose Action to perform when the close button is clicked.
 */
@Composable
internal fun LoadingOverlayFullScreen(
    modifier: Modifier = Modifier,
    closeButtonText: String,
    onClose: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBackground)
            .zIndex(1f)
            .semantics {
                testTag = "preparing_dialog"
            }
            .pointerInput(true) { // Unlike clickable doesn't cause accessibility action to be announced
                // Consume all touch events to prevent interaction with underlying UI
                detectTapGestures {}
            },
        contentAlignment = Alignment.Center,
    ) {
        LoadingContent(closeButtonText = closeButtonText, onClose = onClose)
    }
}

/**
 * Composable function that displays a loading screen with a progress indicator and a close button.
 *
 * @param closeButtonText Text to display on the close button.
 * @param onClose Action to perform when the close button is clicked.
 */
@Composable
internal fun LoadingContent(closeButtonText: String, onClose: () -> Unit = {}) {
    var showCloseButton by remember { mutableStateOf(false) }
    val delayMs = integerResource(integer.loading_close_button_delay_ms).toLong()

    LaunchedEffect(Unit) {
        delay(delayMs) // 20 seconds wait to show close button
        showCloseButton = true
    }
    BackHandler {
        // Disable back button to keep dialog-like behavior
        if (showCloseButton) {
            onClose()
        }
    }
    val loading = stringResource(string.loading)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.token.background.default.copy(alpha = 0.8f))
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.semantics {
            paneTitle = loading
        }
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary,
            strokeWidth = space.small,
        )

        AnimatedVisibility(
            visible = showCloseButton,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite // Announce when the close button appears
            }
        ) {
            LoadingDelayView(
                onClose = onClose,
                closeButtonText = closeButtonText
            )
        }
    }
}

/**
 * Composable function that displays a message and a close button after a delay.
 *
 * @param onClose Action to perform when the close button is clicked.
 * @param closeButtonText Text to display on the close button.
 */
@Composable
private fun LoadingDelayView(
    onClose: () -> Unit = {},
    closeButtonText: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(top = space.large, bottom = space.large, start = 50.dp, end = 50.dp),
            text = stringResource(string.loading_delay_message),
            color = chatColors.token.content.secondary,
            style = chatTypography.loadingScreenText,
            textAlign = TextAlign.Center
        )
        Button(
            modifier = Modifier
                .sizeIn(minHeight = space.clickableSize)
                .testTag("close_button"),
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = chatColors.token.status.error,
                contentColor = chatColors.token.status.onError
            ),
            shape = chatShapes.chip
        ) {
            Text(text = closeButtonText, style = chatTypography.closeButton)
        }
    }
}

@PreviewLightDark
@Composable
private fun LoadingOverlayFullPreview() {
    ChatTheme {
        LoadingOverlayFullScreen(
            closeButtonText = stringResource(string.close_chat),
            onClose = { /* No action needed for preview */ }
        )
    }
}
