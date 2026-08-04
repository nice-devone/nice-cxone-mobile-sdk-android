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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.EditForm
import com.nice.cxonechat.ui.composable.icons.outlined.Mail
import com.nice.cxonechat.ui.composable.icons.outlined.Rename
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun EndConversationIconForMenu(
    tint: Color = LocalContentColor.current,
    contentDescription: String? = stringResource(string.action_end_conversation),
) {
    Icon(
        imageVector = Default.Close,
        contentDescription = contentDescription,
        tint = tint,
    )
}

@Composable
@NonRestartableComposable
internal fun ChatIcon(contentDescription: String? = stringResource(string.change_thread_name)) {
    Icon(ChatIcons.Rename, contentDescription)
}

@Composable
@NonRestartableComposable
internal fun EditIcon(contentDescription: String? = stringResource(string.change_details_label)) {
    Icon(ChatIcons.EditForm, contentDescription)
}

@Composable
@NonRestartableComposable
internal fun MenuIcon(contentDescription: String? = stringResource(string.livechat_conversation_options)) {
    Icon(Default.Menu, contentDescription)
}

@Composable
@NonRestartableComposable
internal fun SendTranscriptIcon(contentDescription: String? = stringResource(string.send_transcript)) {
    Icon(imageVector = ChatIcons.Mail, contentDescription = contentDescription, modifier = Modifier.size(space.actionMenuIconSize))
}

@PreviewLightDark
@Composable
private fun PreviewAllTopBarIcons() {
    ChatTheme {
        Surface(
            color = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
        ) {
            Column {
                EndConversationIconForMenu(tint = colorScheme.error)
                ChatIcon()
                EditIcon()
                MenuIcon()
                SendTranscriptIcon()
                Icon(Default.MoreVert, contentDescription = stringResource(string.livechat_conversation_options))
            }
        }
    }
}
