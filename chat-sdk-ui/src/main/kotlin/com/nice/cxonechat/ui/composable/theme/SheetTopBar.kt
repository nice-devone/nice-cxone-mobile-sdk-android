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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark

/**
 * Material 3 top app bar for the header of a modally presented sheet.
 *
 * The bar has a fixed height, so an overlong title or subtitle is ellipsized rather than wrapped.
 *
 * Vertical insets are not applied, because the hosting sheet already consumes them; applying them
 * here as well would leave a gap above the title. The horizontal inset aligns the title with the
 * sheet body content.
 *
 * @param title Title of the sheet.
 * @param modifier Modifier for the app bar.
 * @param subtitle Optional subtitle, shown below the title.
 * @param titleModifier Modifier for the title, for test tags and semantics overrides.
 * @param subtitleModifier Modifier for the subtitle, for test tags and semantics overrides.
 * @param navigationIcon Navigation icon displayed at the start of the app bar.
 * @param actions Actions displayed at the end of the app bar.
 * @param containerColor Background color of the app bar, transparent so the sheet shows through.
 * @param contentColor Color of the title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.SheetTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleModifier: Modifier = Modifier,
    subtitleModifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = Color.Transparent,
    contentColor: Color = chatColors.token.content.primary,
) {
    val horizontalInsetPx = with(LocalDensity.current) { space.sheetTopBarStartInset.roundToPx() }
    TopAppBar(
        title = {
            Text(
                text = title,
                style = chatTypography.bottomSheetTitleText,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier,
            )
        },
        subtitle = {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = chatTypography.listPickerBottomSheetSubtitleText,
                    color = chatColors.token.content.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = subtitleModifier,
                )
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = WindowInsets(left = horizontalInsetPx, right = horizontalInsetPx),
        colors = topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
        ),
    )
}

@PreviewLightDark
@Composable
private fun SheetTopBarPreview() {
    ChatTheme {
        Surface {
            ChatTheme.SheetTopBar(title = "Title")
        }
    }
}

@PreviewLightDark
@Composable
private fun SheetTopBarWithSubtitlePreview() {
    ChatTheme {
        Surface {
            ChatTheme.SheetTopBar(title = "Title", subtitle = "Subtitle")
        }
    }
}
