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

import androidx.compose.runtime.Stable
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.FailedToDeliver
import com.nice.cxonechat.message.MessageStatus.Sending
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST_SQUASHED
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO_GROUPED
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.domain.model.Person

@Stable
internal fun getGroupState(
    section: Section,
    i: Int,
): MessageItemGroupState {
    val isLast = i == section.messages.lastIndex
    val currentMessage = section.messages[i]
    val messageSender = currentMessage.sender
    val previousMessage = section.messages.getOrNull(i - 1)
    val nextMessage = section.messages.getOrNull(i + 1)

    return when {
        isFailedMessage(currentMessage) -> handleFailedMessage(nextMessage, messageSender)
        isSoloMessage(section) -> SOLO
        isSoloGrouped(previousMessage, nextMessage) -> SOLO_GROUPED
        isStartOfGroup(i, messageSender, previousMessage, currentMessage) ->
            handleStartOfGroup(isLast, nextMessage, messageSender, currentMessage, previousMessage)

        else -> handleMiddleOrEndOfGroup(
            isLast = isLast,
            nextMessage = nextMessage,
            previousMessage = previousMessage,
            messageSender = messageSender,
            currentMessage = currentMessage
        )
    }
}

/**
 * Checks if the message has a FailedToDeliver status.
 */
private fun isFailedMessage(message: Message) = FailedToDeliver === message.status

/**
 * Handles the group state for failed messages.
 */
private fun handleFailedMessage(
    nextMessage: Message?,
    messageSender: Person?,
) =
    if (FailedToDeliver !== nextMessage?.status && messageSender == nextMessage?.sender) LAST else SOLO

/**
 * Checks if the section contains only one message.
 */
private fun isSoloMessage(
    section: Section,
) = section.messages.size == 1

/**
 * Checks if the current message is surrounded by emoji text.
 */
private fun isSoloGrouped(
    previousMessage: Message?,
    nextMessage: Message?,
) = previousMessage is Message.EmojiText && nextMessage is Message.EmojiText

/**
 * Determines if the current message is the start of a new group.
 * A new group can be forced to start if the previous message is an emoji text message.
 */
private fun isStartOfGroup(
    index: Int,
    messageSender: Person?,
    previousMessage: Message?,
    currentMessage: Message,
) = index == 0 ||
        messageSender != previousMessage?.sender ||
        currentMessage.status > previousMessage.statusNonNull() ||
        previousMessage is Message.EmojiText

/**
 * Handles the group state for the start of a new group.
 */
private fun handleStartOfGroup(
    isLast: Boolean,
    nextMessage: Message?,
    messageSender: Person?,
    currentMessage: Message,
    previousMessage: Message?,
) = when {
    isLast ||
            nextMessage is Message.EmojiText ||
            nextMessage?.sender != messageSender ||
            currentMessage.status < nextMessage.statusNonNull() ->
        if (previousMessage is Message.EmojiText) SOLO_GROUPED else SOLO

    previousMessage is Message.EmojiText -> LAST_SQUASHED
    else -> LAST
}

/**
 * Handles the group state for messages in the middle or end of a group.
 */
private fun handleMiddleOrEndOfGroup(
    isLast: Boolean,
    nextMessage: Message?,
    previousMessage: Message?,
    messageSender: Person?,
    currentMessage: Message,
) =
    if (isLast || isEndOfGroup(nextMessage, previousMessage, messageSender, currentMessage)) {
        if (currentMessage.status > previousMessage.statusNonNull()) {
            SOLO
        } else if (previousMessage is Message.EmojiText) {
            SOLO_GROUPED
        } else {
            FIRST
        }
    } else {
        MIDDLE
    }

/**
 * Determines if the current message is the end of a group.
 */
private fun isEndOfGroup(
    nextMessage: Message?,
    previousMessage: Message?,
    messageSender: Person?,
    currentMessage: Message,
) = FailedToDeliver === nextMessage?.status ||
        previousMessage?.sender == messageSender && nextMessage?.sender != messageSender ||
        nextMessage is Message.EmojiText || previousMessage is Message.EmojiText ||
        currentMessage.status != nextMessage.statusNonNull()

/**
 * Returns the status of a message or a default value [Sending] if null.
 */
private fun Message?.statusNonNull(): MessageStatus = this?.status ?: Sending
