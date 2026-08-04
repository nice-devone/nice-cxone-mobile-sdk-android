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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTED
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.FingerDownArrow
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply
import kotlinx.coroutines.launch

@Composable
internal fun QuickReplyMessage(
    message: QuickReply,
    messageStatusState: MessageStatusState,
    modifier: Modifier = Modifier,
    snackBarHostState: SnackbarHostState = SnackbarHostState(),
) {
    val accessibilityDescription = message.getMessageAccessibility()
    val quickReplyDisableMessage = stringResource(R.string.rich_content_disable_message)
    val clickLabel = stringResource(R.string.accessibility_label_onclick_more_information)
    val coroutineScope = rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.spacedBy(space.medium),
        modifier = modifier
            .clickable(
                enabled = messageStatusState == DISABLED, // Only clickable when disabled, to show detailed status message
                onClickLabel = clickLabel
            ) {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(
                        message = quickReplyDisableMessage,
                        duration = Short,
                        withDismissAction = true
                    )
                }
            }
            .semantics {
                testTag = "quick_reply_message"
                contentDescription = accessibilityDescription
            },
    ) {
        Text(
            text = message.title,
            style = chatTypography.chatMessage,
            modifier = Modifier.semantics {
                hideFromAccessibility() // The title is already included in the overall content description of the message.
            }
        )
        QuickReplyMessageStatus(messageStatusState)
    }
}

@Composable
internal fun QuickReplyOptionSubFrame(
    message: QuickReply,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = space.small)) {
        QuickReplyOptions(message, onOptionSelected = onClick)
    }
}

@Composable
internal fun QuickReplyMessageStatus(messageStatusState: MessageStatusState) {
    val (icon, messageText, textColor) = when (messageStatusState) {
        SELECTED -> Triple(
            Icons.Default.CheckCircleOutline,
            stringResource(R.string.option_selected),
            chatColors.token.brand.primary
        )

        DISABLED -> Triple(
            Icons.Default.ErrorOutline,
            stringResource(R.string.options_unavailable),
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
        modifier = Modifier.testTag("quick_reply_message_status"),
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(space.tooltipIconSize),
            contentDescription = null, // Decorative icon
            tint = textColor
        )

        Text(
            modifier = Modifier.testTag("quick_reply_message_status_text"),
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
        modifier = modifier.testTag("quick_reply_options"),
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
    val snackbarHostState = remember { SnackbarHostState() }
    ChatTheme {
        ChatTheme.Scaffold(snackbarHostState = snackbarHostState) {
            PreviewMessageItemBase {
                PreviewMessageItem(
                    message = QuickReply(UiSdkQuickReply()) {},
                    messageStatusState = DISABLED,
                    snackBarHostState = snackbarHostState,
                )
                PreviewMessageItem(
                    message = QuickReply(UiSdkQuickReply()) {},
                    messageStatusState = SELECTABLE,
                )
                PreviewMessageItem(
                    message = QuickReply(UiSdkQuickReply()) {},
                    messageStatusState = SELECTED,
                )
            }
        }
    }
}
