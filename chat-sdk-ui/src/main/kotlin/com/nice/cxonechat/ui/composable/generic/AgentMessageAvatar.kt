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
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Person

/**
 * Unified agent avatar used for both message bubbles and other agent-identifying UI (ThreadList row, End
 * Session card): [agent]'s image when available, otherwise the initials monogram, otherwise a person-icon
 * placeholder - all three sharing the same [ChatTheme.chatColors] brand background tint. [size] adapts the
 * circle (and the monogram/icon proportionally) to the target UI; it defaults to the message-bubble size.
 */
@Composable
internal fun MessageAvatar(agent: Person?, modifier: Modifier = Modifier, size: Dp = ChatTheme.space.messageAvatarSize) {
    val foreground = ChatTheme.chatColors.token.brand.onPrimaryContainer
    val placeholder = forwardingPainter(
        painter = rememberVectorPainter(image = Rounded.Person),
        colorFilter = ColorFilter.tint(foreground)
    )
    val monogram = agent?.monogram
    val scale = size / ChatTheme.space.messageAvatarSize

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ChatTheme.chatColors.token.brand.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        when {
            agent?.imageUrl != null ->
                AsyncImage(
                    model = agent.imageUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("avatarImage"),
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )

            monogram != null ->
                Text(
                    text = monogram,
                    color = foreground,
                    style = ChatTheme.chatTypography.messageAvatarText.let {
                        it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale)
                    },
                    modifier = Modifier.semantics {
                        hideFromAccessibility() // hidden accessibility for Avatar
                    }
                )

            else ->
                Image(
                    painter = placeholder,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("avatarPlaceholder")
                        .padding(ChatTheme.space.messageAvatarIconPadding * scale),
                    contentScale = ContentScale.Fit,
                )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO or UI_MODE_TYPE_NORMAL)
@Composable
private fun DayPreviewMessageAvatar() {
    ChatTheme {
        Column {
            MessageAvatar(Person(firstName = "Some", lastName = "User"))
            MessageAvatar(Person())
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
private fun NightPreviewMessageAvatar() {
    ChatTheme {
        Column {
            MessageAvatar(Person(firstName = "Some", lastName = "User"))
            MessageAvatar(Person())
        }
    }
}
