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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.IconMenuItem

@Composable
internal fun ShowArchivedThreadMenu(displayEndConversation: () -> Unit) {
    IconMenuItem(
        text = string.livechat_conversation_options,
        onClick = remember { displayEndConversation },
        icon = {
            MenuIcon()
        },
        modifier = Modifier.testTag("show_archived_thread_menu_item"),
    )
}

@Composable
internal fun EditThreadValuesMenu(onEditThreadValues: () -> Unit) {
    IconMenuItem(
        text = string.change_details_label,
        onClick = remember { onEditThreadValues },
        icon = {
            EditIcon()
        },
        modifier = Modifier.testTag("edit_thread_custom_values_menu_item"),
    )
}

@Composable
internal fun SendTranscriptMenu(onSendTranscript: () -> Unit) {
    IconMenuItem(
        text = string.send_transcript,
        onClick = remember { onSendTranscript },
        icon = {
            SendTranscriptIcon()
        },
        modifier = Modifier.testTag("send_transcript_menu_item"),
    )
}

@Composable
internal fun ChangeThreadNameMenu(onEditThreadName: () -> Unit) {
    IconMenuItem(
        text = string.change_thread_name,
        onClick = remember { onEditThreadName },
        icon = {
            ChatIcon()
        },
        modifier = Modifier.testTag("change_thread_name_menu_item"),
    )
}

@Composable
internal fun EndConversationMenu(
    threadState: State<ChatThreadState>,
    onClick: () -> Unit,
) {
    IconMenuItem(
        text = string.action_end_conversation,
        onClick = remember { onClick },
        modifier = Modifier.testTag("end_conversation_menu_item"),
        enabled = threadState.value == ChatThreadState.Ready,
        icon = {
            EndConversationIconForMenu()
        },
    )
}
