/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.generic.AgentAvatar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.IconMultiButton
import com.nice.cxonechat.ui.composable.theme.Space
import com.nice.cxonechat.ui.model.EndConversationChoice
import com.nice.cxonechat.ui.model.EndConversationChoice.CLOSE_CHAT
import com.nice.cxonechat.ui.model.EndConversationChoice.NEW_CONVERSATION
import com.nice.cxonechat.ui.model.EndConversationChoice.SHOW_TRANSCRIPT
import kotlin.collections.Map.Entry

@Composable
internal fun ChatTheme.EndConversationDialog(
    assignedAgent: Agent?,
    onDismiss: () -> Unit,
    onUserSelection: (EndConversationChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionMap = prepareChoices()
    val buttonMap = selectionMap.mapValues { prepareToggles(it) }
    Dialog(
        onDismissRequest = onDismiss,
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .height(Min)
                    .padding(space.medium),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val agentName = remember { assignedAgent?.fullName.orEmpty() }
                    val avatarUrl = remember { assignedAgent?.imageUrl }
                    if (avatarUrl != null) {
                        AgentAvatar(avatarUrl, modifier = Modifier.padding(top = space.large))
                    }
                    Text(
                        text = stringResource(R.string.livechat_conversation_closed_message),
                        modifier = Modifier.padding(top = space.large)
                    )
                    AgentName(agentName, space)
                    ChatTheme.IconMultiButton(
                        buttons = buttonMap,
                        modifier = Modifier.padding(space.large)
                    ) {
                        val selected = selectionMap[it]
                        if (selected != null) {
                           onUserSelection(selected)
                           onDismiss()
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AgentName(agentName: String, space: Space) {
    Text(
        text = agentName,
        style = ChatTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = space.large),
    )
}

@Composable
private fun prepareChoices() = mapOf(
    stringResource(id = R.string.livechat_new_chat) to NEW_CONVERSATION,
    stringResource(id = R.string.livechat_show_transcript) to SHOW_TRANSCRIPT,
    stringResource(id = R.string.livechat_close_chat) to CLOSE_CHAT,
)

@Composable
private fun prepareToggles(it: Entry<String, EndConversationChoice>) = when (it.value) {
    SHOW_TRANSCRIPT -> rememberVectorPainter(image = AutoMirrored.Filled.ArrowBackIos)
    NEW_CONVERSATION -> rememberVectorPainter(image = Outlined.ChatBubble)
    CLOSE_CHAT -> rememberVectorPainter(image = Filled.Cancel)
}

@Preview
@Composable
private fun PreviewEndConversationDialog() {
    ChatTheme {
        ChatTheme.EndConversationDialog(
            assignedAgent = PreviewAgent.nextAgent(),
            onDismiss = {},
            onUserSelection = {}
        )
    }
}
