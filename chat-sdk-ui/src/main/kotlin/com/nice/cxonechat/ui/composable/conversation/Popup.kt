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
import com.nice.cxonechat.Popup
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ConversationDialog

@Composable
internal fun Popup(dialog: ConversationDialog.Popup, threadViewModel: ChatThreadViewModel, closeChat: () -> Unit) {
    Popup(
        popup = dialog.popup,
        closeChat = closeChat,
    ) { action ->
        if (action is Action.ReplyButton) threadViewModel.reportReplyButtonClicked(action)
    }
}

@Composable
internal fun Popup(
    popup: Popup,
    closeChat: () -> Unit,
    onClickAction: (Action) -> Unit,
) {
    when (popup) {
        is Popup.InactivityPopup -> InactivityPopup(popup, onClickAction, closeChat)
        else -> Ignored
    }
}
