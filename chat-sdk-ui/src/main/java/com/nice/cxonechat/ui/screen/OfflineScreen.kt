/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.ChatPopup
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.DefaultColors.danger
import com.nice.cxonechat.ui.composable.theme.PopupButton
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar

/**
 * Displays an offline screen when the chat is not connected to the server.
 * It provides a button to dismiss the offline view and return to the previous screen.
 *
 * @param onBackPress Callback to be invoked when the back button is pressed.
 * @param snackbarHostState [SnackbarHostState] to show snackbars.
 * @param modifier [Modifier] to be applied to the screen.
 */
@Composable
internal fun OfflineScreen(
    onBackPress: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBackPress()
    }
    ChatTheme.Scaffold(
        modifier = Modifier
            .testTag("offline_view")
            .then(modifier),
        snackbarHostState = snackbarHostState,
        topBar = {
            ChatTheme.TopBar(
                title = stringResource(id = string.offline),
                navigationIcon = { BackButton(onBackPress) }
            )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            OfflineView(onDismiss = onBackPress)
        }
    }
}

/**
 * Composable that displays the offline view with a message and a button to dismiss it.
 *
 * @param modifier [Modifier] to be applied to the offline view.
 * @param onDismiss Callback to be invoked when the user dismisses the offline view.
 */
@Composable
internal fun OfflineView(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    ChatPopup(
        stringResource(string.offline_banner),
        icon = rememberVectorPainter(ChatIcons.Offline),
        iconContentDescription = stringResource(string.offline),
        subtitle = stringResource(string.offline_message),
        onDismissRequest = onDismiss,
        modifier = Modifier
            .testTag("offline_content_view")
            .then(modifier),
    ) {
        PopupButton(
            text = stringResource(string.disconnect),
            colors = buttonColors(containerColor = danger),
            onClick = onDismiss
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewOfflineContent() {
    ChatTheme {
        ChatTheme.Scaffold(
            topBar = { ChatTheme.TopBar(title = stringResource(id = string.offline)) },
        ) {
            Column {
                OfflineView(modifier = Modifier.padding(it)) {}
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewOfflineView() {
    ChatTheme {
        OfflineScreen({}, SnackbarHostState())
    }
}
