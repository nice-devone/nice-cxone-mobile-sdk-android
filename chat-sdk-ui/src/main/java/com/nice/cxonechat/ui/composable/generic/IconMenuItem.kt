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

import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Simplified version of [DropdownMenuItem] with just the leading icon and text.
 *
 * @param text The string resource to display in the menu item.
 * @param onClick The callback to invoke when the menu item is clicked.
 * @param enabled Whether the menu item is enabled or not.
 * @param icon The composable function to display as the leading icon.
 * @param modifier The modifier to be applied to the menu item.
 */
@Composable
internal fun IconMenuItem(
    @StringRes text: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = { Text(stringResource(text)) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = icon,
        modifier = modifier,
    )
}
