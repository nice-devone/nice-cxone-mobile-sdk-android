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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun SelectAttachmentsTopBar(
    selecting: Boolean,
    toggleSelecting: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.attachments_title),
            style = ChatTheme.chatTypography.surveyTitle,
            modifier = Modifier.padding(start = ChatTheme.space.framePreviewWidth)
        )
        TextButton(
            onClick = toggleSelecting,
            modifier = Modifier.testTag("select_attachments_$selecting")
        ) {
            val text = if (selecting) stringResource(R.string.cancel) else stringResource(R.string.select)
            Text(
                text = text,
                style = ChatTheme.chatTypography.selectAttachmentSelectButtonText,
                modifier = Modifier.animateContentSize()
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TopBarPreview() {
    val selecting = remember { MutableStateFlow(false) }
    ChatTheme {
        Surface {
            SelectAttachmentsTopBar(
                selecting = selecting.collectAsState().value
            ) {
                selecting.value = !selecting.value
            }
        }
    }
}
