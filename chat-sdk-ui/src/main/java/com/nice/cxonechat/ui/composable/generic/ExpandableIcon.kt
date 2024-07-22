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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.LocalSpace

@Composable
internal fun ExpandableIcon(expanded: Boolean, onClick: () -> Unit) {
    val modifier = if (expanded) {
        Modifier
    } else {
        Modifier.rotate(-90f)
    }
        .clickable(onClick = onClick)
        .size(height = LocalSpace.current.clickableSize, width = LocalSpace.current.clickableSize)

    Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = stringResource(id = string.content_description_tree_field_node_icon_expandable),
        modifier = modifier
    )
}
