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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.DISABLED
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.EmojiText
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import kotlinx.coroutines.launch

@Composable
internal fun MessageStatusContentHandler(
    message: Message,
    messageStatusState: MessageStatusState,
    snackBarHostState: SnackbarHostState,
    setShowListPickerDialog: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val quickReplyDisableMessage = stringResource(R.string.quick_reply_disable_message)
    val unsupportedMessage = stringResource(R.string.unsupported_message)
    MessageStatusContent(
        message = message,
        messageStatusState = messageStatusState,
        onClick = {
            when (message.contentType) {
                ContentType.QuickReply ->
                    if (messageStatusState == DISABLED) {
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = quickReplyDisableMessage,
                                duration = Short,
                                withDismissAction = true
                            )
                        }
                    }
                ContentType.ListPicker ->
                    if (messageStatusState == SELECTABLE) {
                        setShowListPickerDialog(true)
                    } else {
                        // No action is taken when messageStatusState is not SELECTABLE.
                        // This is the intended behavior.
                    }
                ContentType.Unsupported ->
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = unsupportedMessage,
                            duration = Short,
                            withDismissAction = true
                        )
                    }
                else -> {}
            }
        }
    )
}

@Composable
internal fun EmojiMessage(message: EmojiText, paddingValues: PaddingValues) {
    Text(
        text = message.text,
        modifier = Modifier
            .testTag("emoji_message")
            .padding(paddingValues),
        style = chatTypography.chatEmojiMessage,
    )
}

@Composable
internal fun TextMessage(message: Text, modifier: Modifier) {
    AutoLinkedText(
        text = message.text,
        modifier = Modifier
            .testTag("text_message")
            .then(modifier),
        style = chatTypography.chatMessage,
    )
}

@Composable
internal fun FallbackText(message: Unsupported, modifier: Modifier) {
    Text(
        text = message.text ?: stringResource(R.string.text_unsupported_message_type),
        modifier = Modifier
            .testTag("fallback_text")
            .then(modifier),
        style = chatTypography.unsupportedMessageText,
        color = chatColors.token.content.primary
    )
}
