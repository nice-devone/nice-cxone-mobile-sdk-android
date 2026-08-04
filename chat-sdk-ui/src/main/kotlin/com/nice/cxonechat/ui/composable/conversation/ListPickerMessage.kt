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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.PressFinger
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker

@Composable
internal fun ListPickerMessage(
    message: ListPicker,
    messageStatusState: MessageStatusState,
    modifier: Modifier = Modifier,
    onMessageClick: () -> Unit,
) {
    val status = messageStatusState.asTriple()
    val messageAccessibility = message.getMessageAccessibility()
    Column(
        modifier
            .clickable(
                enabled = messageStatusState == SELECTABLE,
                onClickLabel = status.second,
                onClick = onMessageClick,
                role = Role.Button,
            )
            .testTag("list_picker_message")
            .semantics(true) {
                contentDescription = messageAccessibility
            }
    ) {
        Column(
            modifier = Modifier
                .padding(space.richListPickerTextPadding)
                .semantics(true) {
                    hideFromAccessibility() // The title and text are read as part of the content description of the message
                }
        ) {
            Text(
                text = message.title,
                color = chatColors.token.content.primary,
                style = chatTypography.listPickerTitle,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = message.text,
                color = chatColors.token.content.secondary,
                modifier = Modifier,
                style = chatTypography.listPickerText
            )
        }
        ListPickerMessageStatus(status)
    }
}

@Composable
internal fun ListPickerMessageStatus(status: Triple<ImageVector, String, Color>) {
    val (icon, messageText, textColor) = status
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .testTag("list_picker_message_status")
            .semantics(true) {
                hideFromAccessibility() // The status is read as part of the clickable label of the message
            },
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(space.tooltipIconSize),
            contentDescription = messageText,
            tint = textColor
        )
        Text(
            text = messageText,
            style = chatTypography.messageStatusText,
            color = textColor
        )
    }
}

@Composable
private fun MessageStatusState.asTriple(): Triple<ImageVector, String, Color> = if (this == SELECTED) {
    Triple(
        Icons.Default.CheckCircleOutline,
        stringResource(string.option_selected),
        chatColors.token.brand.primary
    )
} else {
    Triple(
        ChatIcons.PressFinger,
        stringResource(string.list_picker_open_message),
        chatColors.token.brand.primary
    )
}

/**
 * Determine the current state of the List Picker message options.
 */
internal fun getListPickerState(isNotAnswered: Boolean): MessageStatusState =
    if (isNotAnswered) SELECTABLE else SELECTED

@PreviewLightDark
@Composable
private fun ListPickerMessagePreview() {
    PreviewMessageItemBase(
        message = ListPicker(UiSdkListPicker()) {},
    )
}
