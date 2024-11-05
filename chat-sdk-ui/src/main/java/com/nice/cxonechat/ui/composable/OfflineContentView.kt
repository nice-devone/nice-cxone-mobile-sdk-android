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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PortableWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.LocalChatTypography
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar

@Composable
internal fun OfflineContentView() {
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Icon(
                Icons.Default.PortableWifiOff,
                stringResource(id = string.offline),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(60.dp)
            )
            Text(
                stringResource(string.offline_banner),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = LocalChatTypography.current.offlineBanner
            )
            Text(
                stringResource(string.offline_message),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = LocalChatTypography.current.offlineMessage
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOffline() {
    ChatTheme {
        ChatTheme.Scaffold(
            topBar = { ChatTheme.TopBar(title = stringResource(id = string.offline)) },
        ) {
            OfflineContentView()
        }
    }
}
