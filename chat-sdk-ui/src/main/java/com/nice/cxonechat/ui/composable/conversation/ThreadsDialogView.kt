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
import androidx.compose.runtime.collectAsState
import com.nice.cxonechat.ui.composable.EditThreadNameDialog
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel

@Composable
internal fun ThreadsDialogView(
    threadsViewModel: ChatThreadsViewModel,
) {
    when (val dialog = threadsViewModel.dialogShown.collectAsState().value) {
        ChatThreadsViewModel.Dialog.None -> {
            // Do nothing
        }
        is ChatThreadsViewModel.Dialog.EditThreadName -> EditThreadNameDialog(
            threadName = dialog.thread.name.orDefaultThreadName(),
            onCancel = threadsViewModel::dismissDialog,
            onAccept = {
                threadsViewModel.confirmEditThreadName(dialog.thread, it)
                threadsViewModel.dismissDialog()
            }
        )
    }
}
