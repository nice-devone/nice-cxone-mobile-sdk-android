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

package com.nice.cxonechat

import com.nice.cxonechat.event.thread.LoadThreadMetadataEvent
import com.nice.cxonechat.event.thread.MarkThreadReadEvent
import com.nice.cxonechat.event.thread.SendTranscriptEvent
import com.nice.cxonechat.event.thread.TypingEndEvent
import com.nice.cxonechat.event.thread.TypingStartEvent
import com.nice.cxonechat.exceptions.InvalidParameterException
import com.nice.cxonechat.internal.model.ActionKtx.toEvent
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.internal.model.toEvent as timeSlotToEvent

/**
 * Provides in-one-place interactions to trigger all available events.
 * @see ChatThreadEventHandler
 * */
@Public
object ChatThreadEventHandlerActions {

    /**
     * Mark the thread as read.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.markThreadRead() = trigger(MarkThreadReadEvent())

    /**
     * Notify the server that the user has stopped typing.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.typingEnd() = trigger(TypingEndEvent())

    /**
     * Notify the agent that the user has started typing.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.typingStart() = trigger(TypingStartEvent())

    /**
     * Request additional thread metadata.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.loadMetadata() = trigger(LoadThreadMetadataEvent())

    /**
     * Triggers an action event based on the provided [Action].
     *
     * @param action An instance of [Action] that defines the event to be triggered.
     *  Only supported actions will be processed, others will result in an error.
     *  Supported actions include:
     *  * [Action.ReplyButton]
     * @throws InvalidParameterException if the action is not supported.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.triggerAction(action: Action) {
        val event = action.toEvent()
            ?: throw InvalidParameterException("Action ${action::class.simpleName} is not supported.")
        trigger(event)
    }

    /**
     * Send the chat transcript to the specified email address.
     *
     * @param email The email address to which the transcript will be sent.
     * @return [EventResponse] from the server.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.sendTranscript(email: String): EventResponse? {
        val event = SendTranscriptEvent(email = email)
        return trigger(event)
    }

    /**
     * Sends a postback for the selected time slot (typically in response to a TimePicker [TimeSlot] message).
     *
     * @param timeSlotLocalizedText The text representing the selected time slot, as it was presented to the user.
     * @param timeSlot The selected [TimeSlot] containing the id and other relevant information used to build the postback.
     */
    @JvmStatic
    suspend fun ChatThreadEventHandler.selectTimeSlot(
        timeSlotLocalizedText: String,
        timeSlot: TimeSlot,
    ): EventResponse? {
        val event = timeSlot.timeSlotToEvent(timeSlotLocalizedText)
        return trigger(event)
    }
}
