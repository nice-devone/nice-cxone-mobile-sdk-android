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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
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
import com.nice.cxonechat.ui.domain.model.Person

/**
 * Provides a layout for a message which which includes a frame with
 * a shape based on supplied [position].
 * The layout also includes an optional avatar (used currently only
 * iff [isAgent] is true), leading content slot (unframed), and sub-frame content.
 * The avatar will be displayed only if [avatar] is not null and [position]
 * is either [LAST] or [SOLO].
 * When the avatar will be displayed it will be positioned
 */
@Composable
internal fun MessageFrame(
    position: MessageItemGroupState,
    isAgent: Boolean,
    modifier: Modifier = Modifier,
    avatar: Person? = null,
    colors: ColorPair,
    showFrame: Boolean,
    leadingContent: @Composable () -> Unit = {},
    subFrameContent: @Composable () -> Unit = {},
    framedContent: @Composable () -> Unit,
) {
    val showAvatar = avatar != null && position in listOf(LAST, SOLO)
    val paddingBottom = if (!showAvatar) 0.dp else space.messageAvatarSize / 2 + space.small
    val currentDirection = LocalLayoutDirection.current
    val direction = if (isAgent) currentDirection else currentDirection.inverse()
    val offset = DpOffset(
        x = space.messageAvatarSize / 2 * -1,
        y = space.messageAvatarSize / 2,
    )
    // Inverse the layout direction for the frame if the message is from the client, so we don't have to use different offsets/paddings
    CompositionLocalProvider(value = LocalLayoutDirection provides direction) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .padding(bottom = paddingBottom)
            ) {
                MainSlot(showFrame, position, colors, modifier, currentDirection, framedContent, subFrameContent)
                if (showAvatar) {
                    MessageAvatar(
                        avatar,
                        modifier = Modifier.offset(offset.x, offset.y)
                    )
                }
            }
            LeadingSlot(showAvatar, offset, leadingContent)
        }
    }
}

@Composable
private fun LeadingSlot(showAvatar: Boolean, offset: DpOffset, leadingContent: @Composable (() -> Unit)) {
    Box(
        modifier = Modifier
            .offset(y = if (showAvatar) -offset.y else 0.dp)
    ) {
        leadingContent()
    }
}

@Composable
private fun MainSlot(
    showFrame: Boolean,
    position: MessageItemGroupState,
    colors: ColorPair,
    modifier: Modifier,
    currentDirection: LayoutDirection,
    framedContent: @Composable (() -> Unit),
    subFrameContent: @Composable (() -> Unit),
) {
    Column {
        AnimatedContent(showFrame) { show: Boolean ->
            if (show) {
                Frame(position, colors.background, modifier) {
                    Content(currentDirection, framedContent)
                }
            } else {
                Content(currentDirection, framedContent)
            }
        }
        subFrameContent()
    }
}

@Composable
private fun Content(currentDirection: LayoutDirection, framedContent: @Composable (() -> Unit)) {
    // Use the current layout direction for the content
    CompositionLocalProvider(value = LocalLayoutDirection provides currentDirection) {
        framedContent()
    }
}

private fun LayoutDirection.inverse() = when (this) {
    Ltr -> LayoutDirection.Rtl
    else -> Ltr
}

@Composable
private fun Frame(
    position: MessageItemGroupState,
    color: Color,
    modifier: Modifier,
    framedContent: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = position.stateToShape(toAgent = false),
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
@PreviewLightDark
@Composable
internal fun PreviewMessageFrame() {
    ChatTheme {
        Surface(modifier = Modifier.padding(space.medium)) {
            LazyColumn(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(start = space.medium, end = space.medium)
                    .fillMaxSize()
            ) {
                for (direction in MessageDirection.entries) {
                    val isAgent = direction != ToAgent

                    for (state in MessageItemGroupState.entries) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = space.medium, vertical = space.xSmall),
                                horizontalArrangement = if (isAgent) Arrangement.Start else Arrangement.End,
                            ) {
                                MessageFrame(
                                    position = state,
                                    isAgent = isAgent,
                                    avatar = Person(firstName = "Some", lastName = "User").takeIf { isAgent },
                                    colors = ChatTheme.chatColors.agent,
                                    showFrame = true,
                                    subFrameContent = {
                                        if (state == MIDDLE && isAgent) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(space.medium)) {
                                                FilterChip(false, {}, { Text("Option 1") })
                                                FilterChip(true, {}, { Text("Option 2") })
                                            }
                                        }
                                    },
                                    leadingContent = {
                                        if (state == SOLO) {
                                            LeadingShareAttachmentsIcon(onClick = {})
                                        }
                                    }
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
