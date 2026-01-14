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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.PressFinger
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker

@Composable
internal fun ListPickerMessage(
    message: ListPicker,
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
                .padding(space.richListPickerTextPadding),
            horizontalAlignment = Alignment.Start,
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
    }
}

@Composable
internal fun ActionListItem(
    action: ReplyButton,
    isSelected: Boolean,
    onSelect: (Action) -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) chatColors.token.background.surface.emphasis else chatColors.token.background.surface.subtle,
        ),
        headlineContent = {
            Text(
                action.text,
                modifier = if (action.media != null) Modifier.padding(start = 0.dp) else Modifier
            )
        },
        leadingContent = {
            if (action.media != null) {
                val placeholder = forwardingPainter(
                    painter = rememberVectorPainter(image = Icons.Default.Image),
                    colorFilter = ColorFilter.tint(colorScheme.onBackground)
                )
                SubcomposeAsyncImage(
                    model = action.media.url,
                    loading = { Image(painter = placeholder, contentDescription = null) },
                    error = { Image(painter = placeholder, contentDescription = null) },
                    contentDescription = action.description,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(string.action_was_selected),
                    tint = chatColors.token.brand.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = { onSelect(action) }),
    )
}

@Composable
internal fun ListPickerMessageStatus(messageStatusState: MessageStatusState, onClick: () -> Unit) {
    val (icon, messageText, textColor) = if (messageStatusState == SELECTED) {
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .testTag("list_picker_message_status")
            .clickable {
                if (messageStatusState == SELECTABLE) {
                    onClick()
                }
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

/**
 * Determine the current state of the quick reply options.
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
