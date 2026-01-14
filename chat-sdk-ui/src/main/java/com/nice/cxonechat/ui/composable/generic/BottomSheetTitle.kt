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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.conversation.EndConversationIcon
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Expired
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun BottomSheetTitle(
    message: String,
    modifier: Modifier = Modifier,
    bottomContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space.medium, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = space.xl)
            .fillMaxWidth()
    ) {
        Column(
            Modifier
                .weight(1f)
        ) {
            Text(
                text = message,
                style = chatTypography.bottomSheetTitleText,
                modifier = Modifier
                    .testTag("top_title"),
                color = chatColors.token.content.primary
            )
            if (bottomContent != null) {
                Spacer(Modifier.size(space.small))
                bottomContent()
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TitlePreview_Default() {
    ChatTheme {
        BottomSheetTitle(message = "Bottom Sheet Title")
    }
}

@Preview(showBackground = true, name = "With Trailing Icon")
@Composable
private fun TitlePreview_Trailing() {
    ChatTheme {
        BottomSheetTitle(
            message = "Bottom Sheet Title",
            trailingContent = {
                EndConversationIcon()
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun BottomSheetTitlePreview() {
    ChatTheme {
        BottomSheetTitle(message = "Bottom Sheet Title")
    }
}

@PreviewLightDark
@Composable
private fun Subtitle_Preview() {
    ChatTheme {
        Surface {
            BottomSheetTitle(
                message = "Bottom Sheet Title",
                bottomContent = {
                    Text("Text\nTExt aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa")
                },
                trailingContent = {
                    Icon(
                        imageVector = ChatIcons.Expired,
                        contentDescription = null,
                    )
                }
            )
        }
    }
}
