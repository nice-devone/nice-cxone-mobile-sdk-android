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

import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R.string

@Composable
internal fun EndConversationIcon(tint: Color = LocalContentColor.current) {
    Icon(
        imageVector = Default.Cancel,
        contentDescription = stringResource(string.action_end_conversation),
        tint = tint,
    )
}

@Composable
@NonRestartableComposable
internal fun ChatIcon() {
    Icon(AutoMirrored.Filled.Chat, stringResource(string.change_thread_name))
}

@Composable
@NonRestartableComposable
internal fun EditIcon() {
    Icon(Default.Edit, stringResource(string.change_details_label))
}

@Composable
@NonRestartableComposable
internal fun MenuIcon() {
    Icon(Default.Menu, stringResource(string.livechat_conversation_options))
}
