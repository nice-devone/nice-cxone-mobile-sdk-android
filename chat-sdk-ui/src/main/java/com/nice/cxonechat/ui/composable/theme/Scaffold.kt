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

package com.nice.cxonechat.ui.composable.theme

import android.R.drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.BottomAppBar
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.FloatingActionButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil.ImageLoader
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers

@Composable
internal fun ChatTheme.Scaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    backgroundColor: Color = colors.background,
    contentColor: Color = colors.onBackground,
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material.Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        isFloatingActionButtonDocked = isFloatingActionButtonDocked,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        content = content,
    )
}

@Composable
internal fun ChatTheme.TopBar(
    title: String,
    modifier: Modifier = Modifier,
    logo: Any? = images.logo,
    navigationIcon: @Composable (() -> Unit)? = null,
    backgroundColor: Color = colors.primary,
    contentColor: Color = colors.onPrimary,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            TopBarTitle(logo, title)
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
    )
}

@Composable
internal fun ChatTheme.BottomBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = colors.primary,
    contentColor: Color = colors.onPrimary,
    elevation: Dp = AppBarDefaults.BottomAppBarElevation,
    content: @Composable RowScope.() -> Unit
) {
    BottomAppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        content = content
    )
}

@Composable
private fun ChatTheme.TopBarTitle(logo: Any?, title: String) {
    val size = space.titleBarLogoSize
    val padding = Dp(space.titleBarLogoPadding / LocalContext.current.resources.displayMetrics.density)

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                modifier = Modifier
                    .size(size)
                    .padding(padding),
                contentDescription = null,
                imageLoader = ImageLoader.Builder(LocalContext.current).interceptorDispatcher(Dispatchers.IO).build(),
                placeholder = if (LocalInspectionMode.current) { // Default mipmap has issues in preview.
                    painterResource(id = drawable.ic_dialog_map)
                } else {
                    null
                },
            )
            SmallSpacer()
        }
        Text(title)
    }
}

@Composable
internal fun ChatTheme.Fab(
    icon: Painter,
    @StringRes contentDescription: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = colors.primary,
    contentColor: Color = colors.onPrimary,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
    FloatingActionButton(
        onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription?.let { stringResource(id = it) },
            tint = colors.onPrimary
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScaffoldPreview() {
    ChatTheme {
        ChatTheme.Scaffold(
            topBar = {
                ChatTheme.TopBar(
                    title = "Top Bar",
                )
            },
            floatingActionButton = {
                ChatTheme.Fab(rememberVectorPainter(Icons.Default.Add), contentDescription = null, onClick = { })
            }
        ) {
            Surface(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(ChatTheme.colors.background),
                contentColor = ChatTheme.colors.onBackground
            ) {
                Text("Freddie")
            }
        }
    }
}
