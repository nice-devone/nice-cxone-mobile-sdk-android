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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.composable.conversation.ContentType.AudioAttachment
import com.nice.cxonechat.ui.composable.conversation.ContentType.ListPicker
import com.nice.cxonechat.ui.composable.conversation.ContentType.QuickReply
import com.nice.cxonechat.ui.composable.conversation.ContentType.RichLink
import com.nice.cxonechat.ui.composable.conversation.ContentType.TimePicker
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST_SQUASHED
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO_GROUPED
import com.nice.cxonechat.ui.composable.generic.MessageAvatar
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.audioCellPlaybackRowWidth
import com.nice.cxonechat.ui.composable.theme.isWideScreen
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.domain.model.Person

/**
 * Provides a layout for a message which includes a frame with
 * a shape based on supplied [position].
 * The layout also includes an optional avatar (used currently only
 * iff [isAgent] is true), trailing content slot (unframed), and sub-frame content.
 * The avatar will be displayed only if [avatar] is not null and [position]
 * is either [LAST] or [SOLO].
 * When the avatar will be displayed it will be positioned at the bottom-start
 * of the message frame with an offset of half its size.
 *
 * @param position Determines the message bubble shape (first, middle, last, solo, etc.)
 * @param messageContentType Type of content (QuickReply, RichLink, ListPicker, etc.)
 * @param isAgent Whether the message is from an agent or client
 * @param modifier Modifier to be applied to the root Row composable
 * @param avatar Optional person avatar displayed only for agent messages at certain positions
 * @param colors Color configuration for the message bubble and text
 * @param showFrame Whether to show the message frame/bubble background
 * @param trailingContent Optional composable slot for content displayed after (from edge to center) the message frame (e.g., action icons)
 * @param subFrameContent Optional composable slot for content displayed below the message frame
 * @param framedContent Composable lambda containing the main message content to be displayed within the frame
 */
@Composable
internal fun MessageFrame(
    position: MessageItemGroupState,
    messageContentType: ContentType,
    isAgent: Boolean,
    modifier: Modifier = Modifier,
    avatar: Person? = null,
    colors: ColorPair,
    showFrame: Boolean,
    trailingContent: @Composable () -> Unit = {},
    subFrameContent: @Composable () -> Unit = {},
    framedContent: @Composable () -> Unit,
) {
    val showAvatar = avatar != null && position in listOf(LAST, SOLO)
    val avatarContentTypes by lazy { setOf(QuickReply, RichLink, ListPicker, TimePicker) }
    val paddingBottom = when {
        !showAvatar && messageContentType in avatarContentTypes && position in listOf(LAST, SOLO) -> space.medium
        !showAvatar -> 0.dp
        else -> space.messageAvatarSize / 2 + space.small
    }
    val currentDirection = LocalLayoutDirection.current
    val direction = if (isAgent) currentDirection else currentDirection.inverse()
    val offset = DpOffset(
        x = space.messageAvatarSize / 2 * -1,
        y = space.messageAvatarSize / 2,
    )
    val avatarGutter = avatarGutterSize(isAgent)
    // Inverse the layout direction for the frame if the message is from the client, so we don't have to use different offsets/paddings
    CompositionLocalProvider(value = LocalLayoutDirection provides direction) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space.semiLarge),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(start = avatarGutter)
                .semantics {
                    isTraversalGroup = true
                },
        ) {
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .padding(bottom = paddingBottom)
            ) {
                MainSlot(showFrame, position, messageContentType, colors, currentDirection, framedContent, subFrameContent)
                if (showAvatar) {
                    MessageAvatar(
                        avatar,
                        modifier = Modifier
                            .offset(offset.x, offset.y)
                            .testTag("message_avatar")
                            .semantics {
                                traversalIndex = -1f
                            }
                    )
                }
            }
            TrailingSlot(showAvatar, offset, trailingContent)
        }
    }
}

/**
 * Room the frame gives up at its leading edge for the avatar that `Modifier.offset` draws half outside it without
 * reserving layout space, so it lands on the content column's edge the way the wide-screen design draws it. A compact
 * window gives up none: its design puts the bubble on the column's edge and lets the avatar overhang into the screen
 * margin. Taken for every agent message rather than only the one carrying an avatar, so a group stays in one column.
 */
@Composable
private fun avatarGutterSize(isAgent: Boolean): Dp = if (isAgent && isWideScreen()) space.messageAvatarSize / 2 else 0.dp

@Composable
private fun TrailingSlot(showAvatar: Boolean, offset: DpOffset, slotContent: @Composable (() -> Unit)) {
    Box(
        modifier = Modifier
            .offset(y = if (showAvatar) -offset.y + space.small else 0.dp)
            .semantics {
                traversalIndex = 1f
            }
    ) {
        slotContent()
    }
}

@Composable
private fun MainSlot(
    showFrame: Boolean,
    position: MessageItemGroupState,
    messageContentType: ContentType,
    colors: ColorPair,
    currentDirection: LayoutDirection,
    framedContent: @Composable (() -> Unit),
    subFrameContent: @Composable (() -> Unit),
) {
    Column(
        modifier = Modifier.semantics {
            isTraversalGroup = true
        }
    ) {
        AnimatedContent(showFrame) { show: Boolean ->
            if (show) {
                Frame(position, messageContentType, colors.background) {
                    Column {
                        Content(currentDirection, framedContent)
                    }
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
    messageContentType: ContentType,
    color: Color,
    framedContent: @Composable () -> Unit,
) {
    // An audio cell cannot wrap its playback row, so a narrow window widens its bubble rather than clip a control.
    val minimumMaxWidth = if (messageContentType == AudioAttachment) audioCellPlaybackRowWidth() else 0.dp
    Surface(
        modifier = Modifier.widthIn(max = maxOf(messageBubbleMaxWidth(), minimumMaxWidth)),
        shape = position.stateToShape(toAgent = false),
        color = color,
    ) {
        framedContent()
    }
}

/**
 * Maximum width a message bubble - and anything meant to line up with it, such as a quick reply's
 * options row - may grow to: [Space.messageBubbleMaxWidthFraction] of the content column, the same
 * column [wideScreenHorizontalPadding] insets from the window on both edges.
 *
 * Floored at 0.dp, since a window narrower than its own padding would otherwise subtract to negative.
 *
 * @see wideScreenHorizontalPadding
 */
@Composable
internal fun messageBubbleMaxWidth(): Dp {
    val containerWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val contentWidthDp = (containerWidthDp - wideScreenHorizontalPadding() * 2).coerceAtLeast(0.dp)
    return contentWidthDp * space.messageBubbleMaxWidthFraction
}

@Composable
private fun MessageItemGroupState.stateToShape(
    toAgent: Boolean,
) = when (this) {
    FIRST -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleFirstShape else chatShapes.bubbleShapeToClient.bubbleFirstShape
    MIDDLE -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleMiddleShape else chatShapes.bubbleShapeToClient.bubbleMiddleShape
    LAST, LAST_SQUASHED -> if (toAgent) chatShapes.bubbleShapeToAgent.bubbleLastShape else chatShapes.bubbleShapeToClient.bubbleLastShape
    SOLO, SOLO_GROUPED -> chatShapes.bubbleSoloShape
}

@Suppress("CognitiveComplexMethod")
@PreviewLightDark
@Composable
internal fun PreviewMessageFrame() {
    ChatTheme {
        Surface(
            modifier = Modifier
                .padding(space.medium)
                .semantics { testTagsAsResourceId = true },
            color = chatColors.token.background.default,
        ) {
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
                                    messageContentType = QuickReply,
                                    isAgent = isAgent,
                                    avatar = Person(firstName = "Some", lastName = "User").takeIf { isAgent },
                                    colors = chatColors.agent,
                                    showFrame = true,
                                    subFrameContent = {
                                        if (state == MIDDLE && isAgent) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(space.medium)) {
                                                FilterChip(false, {}, { Text("Option 1") })
                                                FilterChip(true, {}, { Text("Option 2") })
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        if (state == SOLO) {
                                            ShareAttachmentsIcon(contentDescription = "Share attachment", onClick = {})
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
