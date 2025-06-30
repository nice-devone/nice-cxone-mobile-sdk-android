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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.generic.ChatPopup
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.AvatarWaiting
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.DefaultColors
import com.nice.cxonechat.ui.composable.theme.PopupButton
import com.nice.cxonechat.ui.domain.model.EndConversationChoice
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.CLOSE_CHAT
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.NEW_CONVERSATION
import com.nice.cxonechat.ui.domain.model.EndConversationChoice.SHOW_TRANSCRIPT

@Composable
internal fun EndConversation(
    assignedAgent: State<Agent?>,
    onDismiss: () -> Unit,
    onUserSelection: (EndConversationChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val agentName by remember {
        derivedStateOf {
            assignedAgent.value?.fullName.orEmpty()
        }
    }
    val title = if (agentName.isNotEmpty()) {
        stringResource(R.string.livechat_conversation_closed_message)
    } else {
        stringResource(R.string.livechat_conversation_closed_message_no_agent)
    }
    ChatPopup(
        title = title,
        icon = rememberVectorPainter(ChatIcons.AvatarWaiting),
        subtitle = agentName.takeIf { it.isNotEmpty() },
        onDismissRequest = onDismiss,
        modifier = Modifier
            .testTag("end_conversation")
            .then(modifier),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space.medium)
        ) {
            val onClick: (EndConversationChoice) -> Unit = {
                onUserSelection(it)
                onDismiss()
            }
            ActionButton(
                text = stringResource(id = R.string.livechat_new_chat),
                choice = NEW_CONVERSATION,
                onClick = onClick,
                modifier = Modifier.testTag("start_new_chat_button")
            )
            ActionButton(
                text = stringResource(id = R.string.livechat_show_transcript),
                choice = SHOW_TRANSCRIPT,
                onClick = onClick,
                modifier = Modifier.testTag("back_to_conversation_button")
            )
            ActionButton(
                text = stringResource(id = R.string.livechat_close_chat),
                choice = CLOSE_CHAT,
                onClick = onClick,
                modifier = Modifier.testTag("close_chat_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DefaultColors.danger),
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    choice: EndConversationChoice,
    onClick: (EndConversationChoice) -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    PopupButton(
        text = text,
        onClick = { onClick(choice) },
        colors = colors,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun PreviewEndConversationDialog() {
    ChatTheme {
        EndConversation(
            assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
            onDismiss = {},
            onUserSelection = {}
        )
    }
}
