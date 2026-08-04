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

/*
 * Helper composables for TreeField moved out to reduce function count in TreeField.kt
 */

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.LocalChatColors

@Composable
internal fun nodeColor(isSelected: Boolean): Color =
    if (isSelected) LocalChatColors.current.token.background.surface.emphasis else Color.Unspecified

@Composable
internal fun SelectedIcon(label: String) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = stringResource(
            id = string.content_description_icon_is_selected,
            formatArgs = arrayOf(label)
        ),
        tint = colorScheme.primary,
    )
}

@Composable
internal fun TrailingIcon(expanded: Boolean, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.ArrowDropDown, null, modifier.rotate(if (!expanded) -90f else 0f))
}
