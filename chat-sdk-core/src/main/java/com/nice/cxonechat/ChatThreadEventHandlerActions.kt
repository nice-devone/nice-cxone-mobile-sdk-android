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

package com.nice.cxonechat

import com.nice.cxonechat.ChatThreadEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.LoadThreadMetadataEvent
import com.nice.cxonechat.event.thread.MarkThreadReadEvent
import com.nice.cxonechat.event.thread.TypingEndEvent
import com.nice.cxonechat.event.thread.TypingStartEvent
import com.nice.cxonechat.exceptions.InvalidParameterException
import com.nice.cxonechat.internal.model.ActionKtx.toEvent
import com.nice.cxonechat.message.Action

/**
 * Provides in-one-place interactions to trigger all available events.
 * @see ChatThreadEventHandler
 * */
@Public
object ChatThreadEventHandlerActions {

    /**
     * Mark the thread as read.
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.markThreadRead(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(MarkThreadReadEvent(), listener, errorListener)

    /**
     * Notify the server that the user has stopped typing.
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingEnd(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(TypingEndEvent(), listener, errorListener)

    /**
     * Notify the agent that the user has started typing.
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingStart(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(TypingStartEvent(), listener, errorListener)

    /**
     * Request additional thread metadata.
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.loadMetadata(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(LoadThreadMetadataEvent(), listener, errorListener)

    /**
     * Triggers an action event based on the provided [Action].
     *
     * @param action An instance of [Action] that defines the event to be triggered.
     *  Only supported actions will be processed, others will result in an error.
     *  Supported actions include:
     *  * [Action.ReplyButton]
     *  @param listener An optional listener to be notified when the event is sent.
     *  @param errorListener An optional listener to be notified if an error occurs while sending
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.triggerAction(
        action: Action,
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) {
        val event = action.toEvent()
        if (event == null) {
            errorListener?.onError(
                InvalidParameterException("Action ${action::class.simpleName} is not supported.")
            )
            return
        }
        trigger(event, listener, errorListener)
    }
}
