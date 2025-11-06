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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.EndConversationChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EndConversationBottomSheet(
    assignedAgent: State<Agent?>,
    onDismiss: () -> Unit,
    onUserSelection: (EndConversationChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        it === SheetValue.Expanded
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { Spacer(Modifier.size(16.dp)) },
        containerColor = ChatTheme.chatColors.token.background.surface.subtle,
        contentColor = ChatTheme.chatColors.token.content.primary,
        modifier = modifier
            .testTag("end_conversation_bottom_sheet")
    ) {
        EndConversationContent(
            assignedAgent.value,
            onUserSelection,
            onDismiss
        )
    }
}

@Preview
@Composable
private fun PreviewEndConversationDialog() {
    ChatTheme {
        EndConversationBottomSheet(
            assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
            onDismiss = {},
            onUserSelection = {}
        )
    }
}
