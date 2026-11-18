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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.Shapes
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage
import kotlinx.coroutines.launch

/**
 * By default [tooltipState] is persistent, so the tooltip stays up until the reader taps outside it
 * or presses Back. While it is shown, its window swallows every touch, so nothing under it can be
 * tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnsupportedMessage(
    message: Unsupported,
    modifier: Modifier = Modifier,
    tooltipState: TooltipState = rememberTooltipState(isPersistent = true),
) {
    val coroutineScope = rememberCoroutineScope()
    val messageText = message.text()
    val accessibilityDescription = message.getMessageAccessibility(messageText)
    val onClickLabel = stringResource(R.string.accessibility_label_onclick_more_information)
    val errorMessage = stringResource(R.string.unsupported_message_error)
    UnsupportedMessageTooltip(tooltipState) {
        Column(
            modifier = modifier
                .clickable(onClickLabel = onClickLabel) {
                    coroutineScope.launch { tooltipState.show() }
                }
                .semantics(mergeDescendants = true) {
                    testTag = "unsupported_message"
                    contentDescription = accessibilityDescription
                    error(errorMessage)
                },
            verticalArrangement = Arrangement.spacedBy(space.medium)
        ) {
            Text(
                text = messageText,
                modifier = Modifier.semantics {
                    testTag = "fallback_text"
                    hideFromAccessibility() // The message text is described by the column's content description for accessibility
                },
                style = chatTypography.unsupportedMessageText,
            )
            UnsupportedMessageStatus()
        }
    }
}

/**
 * Explains, on tap, why the message could not be displayed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnsupportedMessageTooltip(
    tooltipState: TooltipState,
    anchor: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                // A taller caret would have its tip clipped: the caret is drawn outside the tooltip's bounds.
                caretShape = TooltipDefaults.caretShape(),
                shape = RoundedCornerShape(space.semiLarge),
                containerColor = chatColors.token.background.surface.subtle,
                contentColor = chatColors.token.content.secondary,
                // The tooltip's fill can match the surface it appears over, so it needs a shadow to stand out.
                shadowElevation = space.tooltipShadowElevation,
            ) {
                // Adds to the content padding PlainTooltip applies, which it does not let you change.
                Text(
                    text = stringResource(R.string.unsupported_message),
                    style = chatTypography.tooltipText,
                    modifier = Modifier
                        .testTag("unsupported_message_tooltip")
                        .padding(horizontal = space.medium, vertical = space.semiLarge),
                )
            }
        },
        state = tooltipState,
        focusable = true,
        enableUserInput = false,
        content = anchor,
    )
}

@Composable
internal fun UnsupportedMessageStatus() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space.small),
        modifier = Modifier.testTag("unsupported_message_status"),
    ) {
        CompositionLocalProvider(LocalContentColor provides chatColors.token.status.error) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                modifier = Modifier
                    .size(space.tooltipIconSize)
                    .testTag("message_status_indicator"),
                contentDescription = null // Decorative icon
            )

            Text(
                text = stringResource(R.string.unsupported_message_status),
                style = chatTypography.messageStatusText,
                modifier = Modifier.testTag("unsupported_message_status_text")
            )
        }
    }
}

@Composable
private fun Unsupported.text() = text ?: stringResource(R.string.text_unsupported_message_type)

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun PreviewUnsupportedMessage() {
    ChatTheme {
        Surface(
            modifier = Modifier
                .systemBarsPadding()
                .padding(space.medium),
            color = chatColors.token.background.default,
            shape = Shapes.large
        ) {
            UnsupportedMessage(
                message = Unsupported(UiSdkUnsupportedMessage()),
                modifier = Modifier.padding(space.messagePadding)
            )
        }
    }
}
