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

package com.nice.cxonechat.ui.composable.conversation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Delivered
import com.nice.cxonechat.message.MessageStatus.FailedToDeliver
import com.nice.cxonechat.message.MessageStatus.Read
import com.nice.cxonechat.message.MessageStatus.Seen
import com.nice.cxonechat.message.MessageStatus.Sending
import com.nice.cxonechat.message.MessageStatus.Sent
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun MessageStatusIndicator(status: MessageStatus, modifier: Modifier = Modifier) {
    when (status) {
        Sending -> SendingIcon(modifier = modifier)
        Sent -> SentIcon(modifier = modifier)
        Delivered -> DeliveredIcon(modifier = modifier)
        FailedToDeliver -> FailedIcon(modifier = modifier)
        Seen, Read -> ReadIcon(modifier = modifier)
    }
}

@Composable
private fun SendingIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_message_sent),
        contentDescription = stringResource(R.string.status_sending),
        modifier = modifier,
        colorFilter = tint(ChatTheme.chatColors.messageSent),
    )
}

@Composable
private fun SentIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_message_sent),
        contentDescription = stringResource(R.string.status_sent),
        modifier = modifier,
        colorFilter = tint(ChatTheme.chatColors.messageSending),
    )
}

@Composable
private fun DeliveredIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_message_delivered),
        contentDescription = stringResource(R.string.status_delivered),
        modifier = modifier,
        colorFilter = tint(ChatTheme.colorScheme.secondary),
    )
}

@Composable
private fun FailedIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_message_error),
        contentDescription = stringResource(R.string.status_failed),
        modifier = modifier,
        colorFilter = tint(ChatTheme.colorScheme.error),
    )
}

@Composable
private fun ReadIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_message_read),
        contentDescription = stringResource(R.string.status_read),
        modifier = modifier,
        colorFilter = tint(ChatTheme.colorScheme.secondary),
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StatusIndicatorPreview() {
    PreviewContent()
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StatusIndicatorNightPreview() {
    PreviewContent()
}

@Composable
private fun PreviewContent() {
    ChatTheme {
        Surface(Modifier.width(200.dp)) {
            Column {
                for (status in MessageStatus.entries) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(status.name)
                        Spacer(Modifier.weight(1.0f))
                        MessageStatusIndicator(status)
                    }
                }
            }
        }
    }
}
