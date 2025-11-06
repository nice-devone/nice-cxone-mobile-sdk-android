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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.FingerDownArrow
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply

@Composable
internal fun QuickReplyMessage(
    message: QuickReply,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message.title, style = chatTypography.chatMessage)
    }
}

@Composable
internal fun QuickReplyOptionSubFrame(
    message: QuickReply,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        Modifier
            .padding(top = space.semiLarge)
            .then(modifier)
    ) {
        QuickReplyOptions(message, onOptionSelected = onClick)
    }
}

@Composable
internal fun QuickReplyMessageStatus(messageStatusState: MessageStatusState, onClick: () -> Unit) {
    val (icon, messageText, textColor) = when (messageStatusState) {
        SELECTED -> Triple(
            Icons.Default.CheckCircleOutline,
            stringResource(R.string.option_selected),
            chatColors.token.brand.primary
        )

        DISABLED -> Triple(
            Icons.Default.ErrorOutline,
            stringResource(R.string.quick_reply_options_unavailable),
            chatColors.token.status.error
        )

        SELECTABLE -> Triple(
            ChatIcons.FingerDownArrow,
            stringResource(R.string.select_option_below),
            chatColors.token.brand.primary
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(start = space.xl, bottom = space.semiLarge, end = space.xl)
            .testTag("quick_reply_message_status")
            .clickable {
                if (messageStatusState == DISABLED) {
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
 * Represents the state of a quick reply option.
 */
@Immutable
internal enum class MessageStatusState {
    /* User can select reply. */
    SELECTABLE,

    /* User has selected reply. */
    SELECTED,

    /* User can no select a reply. */
    DISABLED,
}

@Composable
internal fun QuickReplyOptions(message: QuickReply, onOptionSelected: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .testTag("quick_reply_options")
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var selected: Action? by remember { mutableStateOf(null) }
        CompositionLocalProvider(LocalContentColor provides chatColors.token.brand.primary) {
            ChipGroup(
                actions = message.actions,
                selection = selected,
            ) {
                selected = it
                onOptionSelected()
            }
        }
    }
}

/**
 * Determine the current state of the quick reply options.
 */
internal fun getQuickReplyState(
    isLastMessage: Boolean,
    isMessageExtraAvailable: Boolean,
): MessageStatusState = when {
    isLastMessage -> SELECTABLE
    !isLastMessage && !isMessageExtraAvailable -> SELECTED
    else -> DISABLED
}

@PreviewLightDark
@Composable
private fun QuickReplyMessagePreview() {
    PreviewMessageItemBase(
        message = QuickReply(UiSdkQuickReply()) {},
    )
}
