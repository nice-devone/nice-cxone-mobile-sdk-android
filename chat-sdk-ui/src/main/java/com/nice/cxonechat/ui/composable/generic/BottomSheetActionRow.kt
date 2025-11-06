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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun BottomSheetActionRow(
    text: String,
    textColor: Color = chatColors.token.content.primary,
    onClick: () -> Unit,
    testTag: String,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit) = { },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .height(space.bottomSheetActionRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = space.semiLarge)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(space.bottomSheetActionItemSize),
                contentAlignment = Alignment.Center
            ) {
                if (leadingContent != null) {
                    leadingContent()
                }
            }
            Spacer(modifier = Modifier.width(space.semiLarge))
            Text(
                text = text,
                color = textColor,
                style = chatTypography.bottomSheetActionRowText,
                modifier = Modifier.weight(1f)
            )
        }
        trailingContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionRowPreview_Default() {
    ChatTheme {
        BottomSheetActionRow(
            text = "Default Action",
            onClick = {},
            testTag = "BottomSheetActionRowPreview"
        )
    }
}

@Preview(showBackground = true, name = "With Leading Icon")
@Composable
private fun ActionRowPreview_Leading() {
    ChatTheme {
        BottomSheetActionRow(
            text = "With Leading Icon",
            onClick = {},
            testTag = "BottomSheetActionRowPreviewLeading",
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite"
                )
            }
        )
    }
}

@Preview(showBackground = true, name = "With Trailing Icon")
@Composable
private fun ActionRowPreview_Trailing() {
    ChatTheme {
        BottomSheetActionRow(
            text = "With Trailing Icon",
            onClick = {},
            testTag = "BottomSheetActionRowPreviewTrailing",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = "ArrowRight"
                )
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun ActionRowPreview_BothIcons() {
    ChatTheme {
        Surface {
            BottomSheetActionRow(
                text = "With Both Icons",
                onClick = {},
                testTag = "BottomSheetActionRowPreviewBoth",
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite"
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowRight,
                        contentDescription = "ArrowRight"
                    )
                }
            )
        }
    }
}
