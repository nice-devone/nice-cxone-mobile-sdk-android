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

package com.nice.cxonechat.sample.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.extensions.manifestVersionName
import com.nice.cxonechat.sample.ui.theme.AppTheme

/**
 * The Composable to be displayed in the slide out drawer.
 *
 * @param onUiSettings function to display the UI Settings dialog.
 * @param onSdkSettings function to display the SDK Settings dialog.
 * @param onLogout function to logout on user request.
 */
@Composable
fun Drawer(
    onUiSettings: () -> Unit,
    onSdkSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(AppTheme.space.defaultPadding)
    ) {
        val context = LocalContext.current

        Header()
        HorizontalDivider()
        Item(context.manifestVersionName ?: stringResource(string.default_version_name))
        HorizontalDivider()
        Item(stringResource(string.sdk_settings), onClick = onSdkSettings)
        HorizontalDivider()
        Item(stringResource(id = string.ui_settings), onClick = onUiSettings)
        HorizontalDivider()
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        Item(stringResource(string.logout), onClick = onLogout)
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.height(AppTheme.space.clickableSize * 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.large)
    ) {
        AsyncImage(
            model = R.mipmap.ic_launcher,
            contentDescription = null,
        )

        Text(stringResource(string.app_name), style = AppTheme.typography.headlineMedium)
    }
}

@Composable
private fun Item(
    text: String,
    icon: Painter? = null,
    onClick: (() -> Unit)? = null
) {
    val clickable = onClick != null
    val modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier

    Row(
        modifier = modifier
            .height(AppTheme.space.clickableSize),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.large)
    ) {
        icon?.let {
            Image(painter = icon, contentDescription = null)
        }
        Text(text)

        if(clickable) {
            Spacer(Modifier.weight(1f))
            Icon(Filled.KeyboardArrowRight, null)
        }
    }
}

@Preview
@Composable
private fun DrawerPreview() {
    AppTheme {
        Column {
            Drawer({}, {}, {})
        }
    }
}
