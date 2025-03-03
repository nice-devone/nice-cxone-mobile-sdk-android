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

package com.nice.cxonechat.ui.composable.generic

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.model.Person

@Composable
internal fun MessageAvatar(agent: Person, modifier: Modifier = Modifier) {
    val foreground = ChatTheme.chatColors.agentAvatar.foreground
    val placeholder = forwardingPainter(
        painter = rememberVectorPainter(image = Icons.Outlined.AccountCircle),
        colorFilter = ColorFilter.tint(foreground)
    )
    val monogram = agent.monogram

    Box(
        modifier = modifier
            .size(ChatTheme.space.messageAvatarSize)
            .clip(CircleShape)
            .background(ChatTheme.chatColors.agentAvatar.background)
            .padding(0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (monogram != null) {
            Text(
                monogram,
                color = foreground,
                style = ChatTheme.chatTypography.messageAvatarText,
            )
        } else {
            Image(
                placeholder,
                null,
                modifier = Modifier.fillMaxSize().padding(0.dp),
                contentScale = ContentScale.Fit,
            )
        }
        AsyncImage(
            model = agent.imageUrl,
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO or UI_MODE_TYPE_NORMAL)
@Composable
private fun DayPreviewAgentAvatar() {
    ChatTheme {
        Column {
            MessageAvatar(Person(firstName = "Some", lastName = "User"))
            MessageAvatar(Person())
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
private fun NightPreviewAgentAvatar() {
    ChatTheme {
        Column {
            MessageAvatar(Person(firstName = "Some", lastName = "User"))
            MessageAvatar(Person())
        }
    }
}
