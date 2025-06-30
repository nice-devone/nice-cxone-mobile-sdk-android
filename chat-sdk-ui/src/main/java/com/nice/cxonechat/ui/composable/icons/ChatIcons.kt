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

package com.nice.cxonechat.ui.composable.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.filled.AvatarWaiting
import com.nice.cxonechat.ui.composable.icons.filled.Cancel
import com.nice.cxonechat.ui.composable.icons.filled.CancelDark
import com.nice.cxonechat.ui.composable.icons.filled.Expired
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.icons.filled.PlayCircle
import com.nice.cxonechat.ui.composable.icons.notint.Document
import com.nice.cxonechat.ui.composable.icons.notint.DocumentLarge
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * An icon vector accessor.
 */
internal object ChatIcons {

    private var _AllIcons: List<ImageVector>? = null

    internal val AllIcons: List<ImageVector>
        get() {
            if (_AllIcons != null) {
                return _AllIcons!!
            }
            _AllIcons = listOf(
                Cancel,
                CancelDark,
                PlayCircle,
                AvatarWaiting,
                Expired,
                Offline,
                DocumentLarge,
                Document,
            )
            return _AllIcons!!
        }
}

@PreviewLightDark
@Composable
private fun ChatIconsPreview() {
    ChatTheme {
        Surface {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
            ) {
                items(
                    count = ChatIcons.AllIcons.size,
                    key = { index -> ChatIcons.AllIcons[index].name }
                ) { index ->
                    Column(
                        horizontalAlignment = CenterHorizontally,
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .wrapContentHeight()
                            .padding(2.dp)
                    ) {
                        val item = ChatIcons.AllIcons[index]
                        Image(
                            imageVector = item,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp),
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
