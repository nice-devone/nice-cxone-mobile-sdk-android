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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun UnsupportedMessageStatus(onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space.small),
        modifier = Modifier
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .testTag("unsupported_message_status")
            .clickable {
                onClick()
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides chatColors.token.status.error) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                modifier = Modifier
                    .size(space.tooltipIconSize)
                    .testTag("message_status_indicator"),
                contentDescription = stringResource(R.string.unsupported_message_status),
            )

            Text(
                text = stringResource(R.string.unsupported_message_status),
                style = chatTypography.messageStatusText,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewUnsupportedStatus() {
    UnsupportedMessageStatus(onClick = {})
}
