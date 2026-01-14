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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import coil3.compose.AsyncImage
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
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
    BackHandler(onBack = onBackPress)
    ChatTheme.Scaffold(
        modifier = modifier.testTag("offline_view"),
        snackbarHostState = snackbarHostState,
        topBar = {
            ChatTheme.TopBar(
                title = stringResource(id = string.offline),
                navigationIcon = { BackButton(onBackPress) }
            )
        }
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun OfflineView(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("offline_content_view")
            .width(IntrinsicSize.Max)
            .fillMaxHeight()
    ) {
        HeaderBar()
        // Decide whether to show the top app bar based on window size.
        val showImage = currentWindowDpSize().height >= space.offlineImageMinimumScreenHeight
        AnimatedVisibility(
            visible = showImage,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                R.drawable.offline,
                contentDescription = stringResource(string.offline),
                modifier = Modifier
                    .testTag("offline_image")
                    .padding(horizontal = space.offlineImageHorizontalPadding)
            )
        }
        PopupButton(
            modifier = Modifier
                .testTag("offline_close_button")
                .padding(space.xxl)
                .requiredHeight(space.clickableSize),
            text = stringResource(string.chat_offline_action_close),
            colors = buttonColors(
                containerColor = chatColors.token.status.error,
                contentColor = chatColors.token.status.onError
            ),
            onClick = onDismiss
        )
    }
}

@Composable
internal fun HeaderBar() {
    Surface(
        color = Color.Unspecified,
        shape = chatShapes.headerBarShape,
        modifier = Modifier.padding(bottom = space.xxl)
    ) {
        ListItem(
            modifier = Modifier.testTag("offline_header_bar"),
            colors = ListItemDefaults.colors(
                containerColor = chatColors.token.status.errorContainer,
                headlineColor = chatColors.token.content.primary,
                supportingColor = chatColors.token.content.secondary,
            ),
            headlineContent = {
                Text(stringResource(string.offline_banner), style = chatTypography.offlineBannerText)
            },
            supportingContent = {
                Text(stringResource(string.offline_message), style = chatTypography.offlineSupportingText)
            },
            leadingContent = {
                OfflineIcon()
            }
        )
    }
}

@Composable
private fun OfflineIcon() {
    Box(
        modifier = Modifier
            .padding(vertical = space.large, horizontal = space.medium)
            .size(space.offlineIconSize)
            .background(color = chatColors.token.status.error, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.Offline,
            contentDescription = stringResource(string.offline),
            tint = chatColors.token.status.onError,
            modifier = Modifier
                .fillMaxSize()
                .padding(space.offlineLeadingIconPadding)
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
@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun PreviewOfflineView() {
    ChatTheme {
        OfflineScreen({}, SnackbarHostState())
    }
}
