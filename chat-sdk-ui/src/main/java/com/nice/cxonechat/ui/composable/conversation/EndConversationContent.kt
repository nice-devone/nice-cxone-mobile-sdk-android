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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.generic.AgentAvatar
import com.nice.cxonechat.ui.composable.generic.BottomSheetActionRow
import com.nice.cxonechat.ui.composable.generic.BottomSheetTitle
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.ArrowLeft
import com.nice.cxonechat.ui.composable.icons.outlined.MessageChatCircle
import com.nice.cxonechat.ui.composable.icons.outlined.MessageXCircle
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.EndConversationChoice
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.CLOSE_CHAT
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.NEW_CONVERSATION
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.SHOW_TRANSCRIPT

@Composable
internal fun EndConversationContent(
    agentState: Agent?,
    onUserSelection: (EndConversationChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val agentName = agentState?.fullName.orEmpty()
    val agentImageUrl = agentState?.imageUrl
    val message = if (agentName.isNotEmpty()) {
        stringResource(string.livechat_conversation_closed_message)
    } else {
        stringResource(string.livechat_conversation_closed_message_no_agent)
    }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(top = space.medium, bottom = space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.large, Alignment.CenterVertically)
    ) {
        // Agent Card or Fallback
        if (agentName.isNotEmpty()) {
            BottomSheetTitle(message)
            HorizontalCard(agentName, agentImageUrl)
        } else {
            BottomSheetTitle(message) {
                EndConversationIcon()
            }
        }
        ActionList(onUserSelection, onDismiss)
    }
}

@Composable
internal fun EndConversationIcon() {
    Icon(
        painter = rememberVectorPainter(ChatIcons.MessageXCircle),
        contentDescription = null,
        modifier = Modifier
            .background(color = colorScheme.errorContainer, shape = CircleShape)
            .padding(space.medium)
            .size(space.bottomSheetTitleIconSize),
        tint = colorScheme.onErrorContainer
    )
}

@Composable
private fun ActionList(onUserSelection: (EndConversationChoice) -> Unit, onDismiss: () -> Unit) {
    val dividerColor = chatColors.token.border.default
    val iconMod = Modifier.fillMaxSize()
    Column(modifier = Modifier.fillMaxWidth()) {
        BottomSheetActionRow(
            text = stringResource(string.livechat_new_chat),
            textColor = colorScheme.primary,
            onClick = {
                onUserSelection(NEW_CONVERSATION)
                onDismiss()
            },
            testTag = "start_new_chat_button",
            leadingContent = {
                Icon(
                    painter = rememberVectorPainter(ChatIcons.MessageChatCircle),
                    contentDescription = stringResource(string.livechat_new_chat),
                    tint = colorScheme.primary,
                    modifier = iconMod
                )
            }
        )
        DividerItem(color = dividerColor)
        BottomSheetActionRow(
            text = stringResource(string.livechat_show_transcript),
            textColor = colorScheme.primary,
            onClick = {
                onUserSelection(SHOW_TRANSCRIPT)
                onDismiss()
            },
            testTag = "back_to_conversation_button",
            leadingContent = {
                Icon(
                    painter = rememberVectorPainter(ChatIcons.ArrowLeft),
                    contentDescription = stringResource(string.livechat_show_transcript),
                    tint = colorScheme.primary,
                    modifier = iconMod
                )
            }
        )
        DividerItem(color = dividerColor)
        BottomSheetActionRow(
            text = stringResource(string.livechat_close_chat),
            textColor = chatColors.token.content.secondary,
            onClick = {
                onUserSelection(CLOSE_CHAT)
                onDismiss()
            },
            testTag = "close_chat_button",
            leadingContent = {
                Icon(
                    painter = rememberVectorPainter(Icons.Rounded.Close),
                    contentDescription = stringResource(string.livechat_close_chat),
                    tint = colorScheme.tertiary,
                    modifier = iconMod
                )
            }
        )
    }
}

@Composable
private fun HorizontalCard(
    agentName: String,
    agentImageUrl: String? = null,
) {
    val cardBorderColor = chatColors.token.border.default
    val cardCornerRadius = chatShapes.actionButtonShape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .padding(horizontal = space.large)
            .defaultMinSize(minHeight = space.bottomSheetCardHeight)
            .background(Color.Transparent, cardCornerRadius)
            .border(width = space.bottomSheetBorderWidth, color = cardBorderColor, shape = cardCornerRadius)
            .padding(all = space.large)
            .testTag("agent_card")
            .fillMaxWidth()
    ) {
        AgentAvatar(agentImageUrl, Modifier.background(cardBorderColor, CircleShape))
        Spacer(modifier = Modifier.width(space.large))
        Text(
            text = agentName,
            style = chatTypography.chatAgentName,
            modifier = Modifier.testTag("agent_name"),
            color = chatColors.token.content.primary
        )
    }
}

@Composable
internal fun DividerItem(color: Color) {
    HorizontalDivider(color = color)
}

@PreviewLightDark
@Composable
private fun PreviewContentNoAgent() {
    ChatTheme {
        Box(
            modifier = Modifier
                .background(chatColors.token.background.surface.subtle)
        ) {
            EndConversationContent(
                agentState = null,
                onUserSelection = {},
                onDismiss = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewContent() {
    ChatTheme {
        Surface(
            modifier = Modifier.systemBarsPadding(),
            color = chatColors.token.background.surface.subtle
        ) {
            EndConversationContent(
                agentState = PreviewAgent.nextAgent(),
                onUserSelection = {},
                onDismiss = {},
            )
        }
    }
}
