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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.ChatPopupContent
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.AvatarWaiting
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun PositionInQueue(
    position: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints {
        val title = when {
            position == 1 -> stringResource(id = string.position_in_queue_next)
            else -> stringResource(id = string.position_in_queue_cardinal, position)
        }
        ChatPopupContent(
            title = title,
            icon = rememberVectorPainter(ChatIcons.AvatarWaiting),
            modifier = Modifier
                .testTag("position_in_queue")
                .then(modifier),
            subtitle = stringResource(string.position_in_queue_supporting_text),
            collapsePopupHeight = this@BoxWithConstraints.maxHeight * 0.75f
        )
    }
}

@PreviewLightDark
@Composable
private fun PositionInQueue_Preview() {
    ChatTheme {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .height(300.dp)
        ) {
            for (position in 1.until(4)) {
                PositionInQueue(position = position)
            }
        }
    }
}
