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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit.SECONDS

@Composable
internal fun LazyItemScope.LoadMore(modifier: Modifier = Modifier, loadMore: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .animateItem()
            .testTag("Load_More")
            .padding(top = space.medium, bottom = space.xSmall),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = ChatTheme.chatColors.token.content.tertiary
        )
        Text(
            text = stringResource(string.text_loading_more_messages),
            style = ChatTheme.chatTypography.chatLoadMoreCaption,
            color = ChatTheme.chatColors.token.content.tertiary
        )
        LaunchedEffect(key1 = null) { // The whole LoadMore should be removed after recomposition
            while (this.isActive) { // Workaround for issue with scrollToken reset - otherwise loadMore() should suffice
                loadMore()
                delay(SECONDS.toMillis(10)) // Arbitrary value - should be sufficient until the issue is fixed
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewLoadMore() {
    ChatTheme {
        Surface {
            LazyColumn {
                item {
                    LoadMore { }
                }
            }
        }
    }
}
