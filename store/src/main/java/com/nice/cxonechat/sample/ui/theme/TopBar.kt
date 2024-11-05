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

package com.nice.cxonechat.sample.ui.theme

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * An AppTheme'd top/action bar.
 *
 * @param title Title to display.
 * @param navigationIcon Navigation Icon to display on left of bar.
 * @param actions Any actions to display on right of bar.
 * @param containerColor Background color for bar, defaults to AppTheme.colors.primary.
 * @param contentColor Content color for bar, defaults to AppTheme.colors.onPrimary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme.TopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit) = { },
    actions: @Composable RowScope.() -> Unit = { },
    containerColor: Color = colorScheme.primary,
    contentColor: Color = colorScheme.onPrimary,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
        ),
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TopBarPreview() {
    AppTheme {
        AppTheme.TopBar(
            title = "Some Title",
            navigationIcon = {
                IconButton(onClick = { }) {
                    Icon(Filled.ArrowBack, null)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ShoppingCart, null)
                }
            }
        )
    }
}
