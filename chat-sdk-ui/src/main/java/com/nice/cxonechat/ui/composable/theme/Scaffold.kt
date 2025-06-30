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

package com.nice.cxonechat.ui.composable.theme

import android.R.drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.Scaffold as M3Scaffold

@Composable
internal fun ChatTheme.Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = colorScheme.background,
    contentColor: Color = colorScheme.onBackground,
    content: @Composable (PaddingValues) -> Unit,
) {
    M3Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = backgroundColor,
        contentColor = contentColor,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.TopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    logo: Any? = null,
    navigationIcon: @Composable () -> Unit = { },
    containerColor: Color = colorScheme.secondary,
    contentColor: Color = colorScheme.onSecondary,
) {
    TopAppBar(
        title = {
            TopBarTitle(logo, title)
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        colors = topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.MediumTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    logo: Any? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = colorScheme.secondary,
    contentColor: Color = colorScheme.onSecondary,
    navigationIcon: @Composable () -> Unit = { },
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumTopAppBar(
        title = {
            TopBarTitle(logo, title)
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ChatTheme.TopBarTitle(logo: Any?, title: String?) {
    val size = space.titleBarLogoSize
    val padding = with(LocalDensity.current) { space.titleBarLogoPadding.toDp() }

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
                imageLoader = ImageLoader.Builder(LocalContext.current).coroutineContext(Dispatchers.IO).build(),
                placeholder = if (LocalInspectionMode.current) { // Default mipmap has issues in preview.
                    painterResource(id = drawable.ic_dialog_map)
                } else {
                    null
                },
            )
            SmallSpacer()
        }
        title?.let { Text(it, style = chatTypography.topBarTitle) }
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
    containerColor: Color = colorScheme.primary,
    contentColor: Color = colorScheme.onPrimary,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
    FloatingActionButton(
        onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription?.let { stringResource(id = it) },
            tint = colorScheme.onPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun ScaffoldPreview(
    @PreviewParameter(LoremIpsum::class) content: String,
) {
    ChatTheme {
        ChatTheme.Scaffold(
            topBar = {
                ChatTheme.TopBar(
                    title = "Title",
                    navigationIcon = { BackButton { } },
                )
            },
            floatingActionButton = {
                ChatTheme.Fab(rememberVectorPainter(Icons.Default.Add), contentDescription = null, onClick = { })
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(content)
            }
        }
    }
}
