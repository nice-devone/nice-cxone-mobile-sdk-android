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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.generic.TypingIndicator
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.model.Person

@Composable
internal fun TypingIndicatorMessage(
    agent: Person,
    modifier: Modifier = Modifier
) {
    val typing = stringResource(R.string.text_agent_typing)

    MessageFrame(
        position = SOLO,
        isAgent = true,
        modifier = modifier,
        avatar = agent,
        colors = ChatTheme.chatColors.agent,
    ) {
        TypingIndicator(
            modifier = Modifier
                .padding(ChatTheme.space.messagePadding)
                .semantics {
                    contentDescription = typing
                }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
internal fun PreviewTypingIndicator() {
    ChatTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TypingIndicatorMessage(
                Person(firstName = "Some", lastName = "User"),
            )
        }
    }
}
