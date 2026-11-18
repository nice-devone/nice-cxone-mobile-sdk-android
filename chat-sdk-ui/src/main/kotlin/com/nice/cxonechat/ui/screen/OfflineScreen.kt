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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import coil3.compose.AsyncImage
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.headerBarContentPadding
import com.nice.cxonechat.ui.composable.headerBarLeadingGap
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.PopupButton
import com.nice.cxonechat.ui.composable.theme.Scaffold

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
    val panelTitle = stringResource(id = string.panel_title_offline)
    ChatTheme.Scaffold(
        modifier = modifier
            .testTag("offline_view")
            .semantics {
                paneTitle = panelTitle
            },
        snackbarHostState = snackbarHostState,
    ) {
        // Two Boxes are needed: one paints the status bar/cutout area, the other holds the inset-padded content.
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .background(chatColors.token.brand.primary)
            )
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
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("offline_content_view")
            .fillMaxSize()
    ) {
        HeaderBar()
        // Decide whether to show the illustration based on window size.
        val showImage = LocalWindowInfo.current.containerDpSize.height >= space.offlineImageMinimumScreenHeight
        AnimatedVisibility(
            visible = showImage,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                R.drawable.offline,
                contentDescription = null, // Decorative image
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .testTag("offline_image")
                    .fillMaxHeight()
                    .padding(horizontal = space.offlineImageHorizontalPadding)
            )
        }
        PopupButton(
            modifier = Modifier
                .testTag("offline_close_button")
                .padding(start = space.xxl, top = space.xxl, end = space.xxl, bottom = space.offlineCloseButtonBottomPadding)
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
    // The bar floats inside a horizontal margin and is capped at componentMaxWidth, so it never touches the
    // window edges and all four of its corners stay visible - hence the inset shape rather than the edge-to-edge one.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(space.large)
    ) {
        Surface(
            color = Color.Unspecified,
            shape = chatShapes.headerBarInsetShape,
            modifier = Modifier.widthIn(max = space.componentMaxWidth)
        ) {
            ListItem(
                // Geometry is stated rather than inherited; see headerBarContentPadding for why.
                modifier = Modifier
                    .testTag("offline_header_bar")
                    .semantics(mergeDescendants = true) { }
                    .heightIn(min = space.headerBarMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = headerBarContentPadding(),
                colors = ListItemDefaults.colors(
                    containerColor = chatColors.token.status.errorContainer,
                ),
                leadingContent = {
                    Box(headerBarLeadingGap()) { OfflineIcon() }
                },
            ) {
                Column {
                    Text(
                        stringResource(string.offline_banner),
                        color = chatColors.token.content.primary,
                        style = chatTypography.offlineBannerText,
                    )
                    Text(
                        stringResource(string.offline_message),
                        color = chatColors.token.content.secondary,
                        style = chatTypography.offlineSupportingText,
                    )
                }
            }
        }
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
            contentDescription = null, // Decorative icon
            tint = chatColors.token.status.onError,
            modifier = Modifier
                .fillMaxSize()
                .padding(space.offlineLeadingIconPadding)
        )
    }
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun PreviewOfflineContent() {
    ChatTheme {
        ChatTheme.Scaffold {
            OfflineView(modifier = Modifier.padding(it)) {}
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
