/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.event.thread.ArchiveThreadEvent
import com.nice.cxonechat.event.thread.LoadThreadMetadataEvent
import com.nice.cxonechat.event.thread.MarkThreadReadEvent
import com.nice.cxonechat.event.thread.TypingEndEvent
import com.nice.cxonechat.event.thread.TypingStartEvent

/**
 * Provides in-one-place interactions to trigger all available events.
 * @see ChatThreadEventHandler
 * */
@Public
object ChatThreadEventHandlerActions {

    /**
     * @see ChatThreadEventHandler.trigger
     * @see ArchiveThreadEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.archiveThread(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ArchiveThreadEvent, listener, errorListener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see MarkThreadReadEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.markThreadRead(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(MarkThreadReadEvent, listener, errorListener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see TypingEndEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingEnd(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(TypingEndEvent, listener, errorListener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see TypingStartEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingStart(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(TypingStartEvent, listener, errorListener)

    /**
     * Send a [LoadThreadMetadataEvent] requesting additional thread information.
     *
     * @see ChatThreadEventHandler.trigger
     * @see LoadThreadMetadataEvent
     */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.loadMetadata(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(LoadThreadMetadataEvent, listener, errorListener)
}
