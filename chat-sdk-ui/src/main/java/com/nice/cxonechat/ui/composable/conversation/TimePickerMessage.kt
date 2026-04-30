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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.PressFinger
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker

@Composable
internal fun TimePickerMessage(
    message: TimePicker,
    modifier: Modifier = Modifier,
    onMessageClick: () -> Unit,
) {
    val clickModifier = Modifier.clickable { onMessageClick() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = clickModifier
                .padding(space.richTimePickerTextPadding),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = message.title,
                color = chatColors.token.content.primary,
                modifier = Modifier,
                style = chatTypography.timePickerText
            )
        }
    }
}

@Composable
internal fun TimePickerMessageStatus(messageStatusState: MessageStatusState, onClick: () -> Unit) {
    val (icon, messageText, textColor) = when (messageStatusState) {
        SELECTED -> Triple(
            Icons.Default.CheckCircleOutline,
            stringResource(string.option_selected),
            chatColors.token.brand.primary
        )

        DISABLED -> Triple(
            Icons.Default.ErrorOutline,
            stringResource(string.options_unavailable),
            chatColors.token.status.error
        )

        SELECTABLE -> Triple(
            ChatIcons.PressFinger,
            stringResource(string.list_picker_open_message),
            chatColors.token.brand.primary
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .testTag("time_picker_message_status")
            .clickable {
                onClick()
            },
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(space.tooltipIconSize),
            contentDescription = null, // Decorative icon, content description is provided by the text next to it.
            tint = textColor
        )
        Text(
            text = messageText,
            style = chatTypography.messageStatusText,
            color = textColor
        )
    }
}

internal fun getTimePickerState(
    isLastMessage: Boolean,
    isMessageExtraAvailable: Boolean,
): MessageStatusState = when {
    isLastMessage -> SELECTABLE
    !isLastMessage && !isMessageExtraAvailable -> SELECTED
    else -> DISABLED
}

@PreviewLightDark
@Composable
private fun TimePickerMessagePreview() {
    PreviewMessageItemBase(
        message = TimePicker(UiSdkTimePicker()),
    )
}
