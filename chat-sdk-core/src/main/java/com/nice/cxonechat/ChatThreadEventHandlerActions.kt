package com.nice.cxonechat

import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ArchiveThreadEvent
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
    ) = trigger(ArchiveThreadEvent, listener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see MarkThreadReadEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.markThreadRead(
        listener: OnEventSentListener? = null,
    ) = trigger(MarkThreadReadEvent, listener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see TypingEndEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingEnd(
        listener: OnEventSentListener? = null,
    ) = trigger(TypingEndEvent, listener)

    /**
     * @see ChatThreadEventHandler.trigger
     * @see TypingStartEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatThreadEventHandler.typingStart(
        listener: OnEventSentListener? = null,
    ) = trigger(TypingStartEvent, listener)

}
