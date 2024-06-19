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

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun AgentAvatar(url: String, modifier: Modifier = Modifier) {
    val placeholder = forwardingPainter(
        painter = rememberVectorPainter(image = Outlined.AccountCircle),
        colorFilter = ColorFilter.tint(ChatTheme.colors.onBackground)
    )

    AsyncImage(
        model = url.ifBlank { null },
        placeholder = placeholder,
        fallback = placeholder,
        contentDescription = null,
        modifier = modifier
            .size(ChatTheme.space.agentImageSize)
            .clip(CircleShape),
        alignment = Alignment.Center,
    )
}

@Preview
@Composable
private fun PreviewAgentAvatar() {
    ChatTheme {
        AgentAvatar(url = "")
    }
}
