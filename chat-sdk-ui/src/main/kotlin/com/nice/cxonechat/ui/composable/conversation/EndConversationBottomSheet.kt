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

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.SheetAnchors
import com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheet
import com.nice.cxonechat.ui.domain.model.EndConversationChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EndConversationBottomSheet(
    assignedAgent: State<Agent?>,
    liveChatAllowTranscript: Boolean,
    onDismiss: () -> Unit,
    onUserSelection: (EndConversationChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly) {
        it === SheetValue.Expanded
    }
    ChatTheme.WideScreenModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // This sheet's design keeps a centred card in landscape, so it opts out of the wide-screen rule.
        sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth,
        // No handle in the design, and Material 3 makes any handle it is given clickable and focusable,
        // so a spacer here would read to screen readers as an unlabelled control. Content pads instead.
        dragHandle = null,
        modifier = modifier
            .testTag("end_conversation_bottom_sheet")
    ) {
        EndConversationContent(
            agentState = assignedAgent.value,
            liveChatAllowTranscript = liveChatAllowTranscript,
            onUserSelection = onUserSelection,
            onDismiss = onDismiss
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewEndConversationDialog() {
    ChatTheme {
        EndConversationBottomSheet(
            assignedAgent = remember { mutableStateOf(PreviewAgent.nextAgent()) },
            liveChatAllowTranscript = true,
            onDismiss = {},
            onUserSelection = {}
        )
    }
}
