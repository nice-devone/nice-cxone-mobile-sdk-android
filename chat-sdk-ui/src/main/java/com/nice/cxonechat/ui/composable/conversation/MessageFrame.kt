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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.generic.MessageAvatar
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.model.Person

@Composable
internal fun MessageFrame(
    position: MessageItemGroupState,
    isAgent: Boolean,
    modifier: Modifier = Modifier,
    avatar: Person? = null,
    colors: ColorPair,
    subFrameContent: @Composable () -> Unit = {},
    framedContent: @Composable () -> Unit,
) {
    val showAvatar = avatar != null && position in listOf(LAST, SOLO)
    val paddingBottom = if (!showAvatar) 0.dp else space.messageAvatarSize + space.small
    val offset = DpOffset(
        x = space.messageAvatarSize / 2 * if (isAgent) -1 else 1,
        y = space.messageAvatarSize / 2,
    )
    Box(
        contentAlignment = if (isAgent) Alignment.BottomStart else Alignment.BottomEnd,
        modifier = Modifier
            .padding(bottom = paddingBottom)
            .then(modifier)
    ) {
        Column {
            Frame(position, isAgent, colors.background, modifier, framedContent)
            subFrameContent()
        }
        if (avatar != null && position in listOf(LAST, SOLO)) {
            MessageAvatar(
                avatar,
                modifier = Modifier.offset(offset.x, offset.y)
            )
        }
    }
}

@Composable
private fun Frame(
    position: MessageItemGroupState,
    isAgent: Boolean,
    color: Color,
    modifier: Modifier,
    framedContent: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = position.stateToShape(toAgent = !isAgent),
        color = color,
    ) {
        framedContent()
    }
}

@Composable
private fun MessageItemGroupState.stateToShape(
    toAgent: Boolean,
) = when (this) {
    FIRST -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleFirstShape else chatShapes.bubbleShapeToClient.bubbleFirstShape
    MIDDLE -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleMiddleShape else chatShapes.bubbleShapeToClient.bubbleMiddleShape
    LAST -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleLastShape else chatShapes.bubbleShapeToClient.bubbleLastShape
    SOLO -> chatShapes.bubbleSoloShape
}

@Suppress("CognitiveComplexMethod")
@Preview(showBackground = true)
@Composable
internal fun PreviewMessageFrame() {
    ChatTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LazyColumn(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(start = space.medium, end = space.medium)
                    .fillMaxSize()
            ) {
                for (direction in MessageDirection.entries) {
                    val isAgent = direction == ToAgent

                    for (state in MessageItemGroupState.entries) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(space.medium),
                                horizontalArrangement = if (isAgent) Arrangement.Start else Arrangement.End
                            ) {
                                MessageFrame(
                                    position = state,
                                    isAgent = direction == ToAgent,
                                    modifier = Modifier.weight(1f),
                                    avatar = Person(firstName = "Some", lastName = "User"),
                                    colors = ChatTheme.chatColors.agent
                                ) {
                                    Text("$direction:$state", modifier = Modifier.padding(space.messagePadding))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
