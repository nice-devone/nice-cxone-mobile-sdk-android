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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun HeaderBar(
    containerColor: Color,
    titleText: String,
    messageText: String,
    leadingContent: @Composable (() -> Unit),
) {
    Surface(
        color = Color.Unspecified,
        shape = chatShapes.headerBarShape,
        modifier = Modifier.padding(bottom = space.headerBarBottomPadding)
    ) {
        ListItem(
            modifier = Modifier.testTag("header_bar"),
            colors = ListItemDefaults.colors(
                containerColor = containerColor,
            ),
            headlineContent = {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = titleText,
                        color = chatColors.token.content.primary,
                        style = chatTypography.headerBarTitle
                    )
                    Text(
                        text = messageText,
                        color = chatColors.token.content.secondary,
                        modifier = Modifier.padding(top = space.small),
                        style = chatTypography.headerBarMessage
                    )
                }
            },
            leadingContent = {
                leadingContent()
            }
        )
    }
}

/* Sample Icon for the leadingContent slot Preview */
@Composable
internal fun LeadingContentPreviewContent() {
    Box(
        modifier = Modifier
            .padding(vertical = space.large, horizontal = space.medium)
            .size(space.positionInQueueIconSize)
            .background(color = chatColors.token.brand.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.Offline,
            contentDescription = stringResource(R.string.offline),
            tint = chatColors.token.status.onError,
            modifier = Modifier
                .fillMaxSize()
                .padding(space.headerBarLeadingIconPadding)
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewHeaderBar() {
    ChatTheme {
        HeaderBar(
            containerColor = chatColors.token.background.surface.emphasis,
            titleText = stringResource(id = R.string.position_in_queue_next),
            messageText = stringResource(id = R.string.position_in_queue_supporting_text),
            leadingContent = { LeadingContentPreviewContent() }
        )
    }
}
