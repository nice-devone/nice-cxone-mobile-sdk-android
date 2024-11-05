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

package com.nice.cxonechat.ui.composable.conversation

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit.SECONDS

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyItemScope.LoadMore(loadMore: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .animateItem(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(string.text_loading_more_messages),
            style = ChatTheme.chatTypography.chatLoadMoreCaption,
            modifier = Modifier.padding(ChatTheme.space.medium)
        )
        LaunchedEffect(key1 = null) { // The whole LoadMore should be removed after recomposition
            while (this.isActive) { // Workaround for issue with scrollToken reset - otherwise loadMore() should suffice
                loadMore()
                delay(SECONDS.toMillis(10)) // Arbitrary value - should be sufficient until the issue is fixed
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
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
