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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.PressFinger
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import kotlinx.coroutines.launch

@Composable
internal fun TimePickerMessage(
    message: TimePicker,
    messageStatusState: MessageStatusState,
    modifier: Modifier = Modifier,
    onMessageClick: () -> Unit,
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val status = messageStatusState.asTriple()
    val messageAccessibility = message.getMessageAccessibility()
    val coroutineScope = rememberCoroutineScope()
    val disableMessage = stringResource(string.rich_content_disable_message)
    val clickLabel = if (messageStatusState == SELECTABLE) {
        status.second
    } else {
        stringResource(string.accessibility_label_status_click_for_info, status.second)
    }
    Column(
        modifier
            .clickable(
                onClickLabel = clickLabel,
                role = Role.Button,
            ) {
                if (messageStatusState == SELECTABLE) {
                    onMessageClick()
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = disableMessage,
                            duration = SnackbarDuration.Short,
                            withDismissAction = true
                        )
                    }
                }
            }
            .semantics(true) {
                testTag = "time_picker_message"
                contentDescription = messageAccessibility
            }
    ) {
        Column(
            modifier = Modifier
                .padding(space.richTimePickerTextPadding)
                .semantics(true) {
                    hideFromAccessibility() // The title and text are read as part of the content description of the message
                }
        ) {
            Text(
                text = message.title,
                color = chatColors.token.content.primary,
                modifier = Modifier,
                style = chatTypography.timePickerText
            )
        }
        TimePickerMessageStatus(status)
    }
}

@Composable
private fun MessageStatusState.asTriple(): Triple<ImageVector, String, Color> = when (this) {
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

@Composable
internal fun TimePickerMessageStatus(status: Triple<ImageVector, String, Color>) {
    val (icon, messageText, textColor) = status
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .semantics(true) {
                testTag = "time_picker_message_status"
                hideFromAccessibility() // The status is read as part of the clickable label of the message
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
