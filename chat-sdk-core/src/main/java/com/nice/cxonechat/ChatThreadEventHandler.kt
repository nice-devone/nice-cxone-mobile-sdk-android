package com.nice.cxonechat

import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.thread.ChatThread

/**
 * Event handler allows for triggering events regarding the [ChatThread] instance
 * it was created for.
 *
 * It has no side effects attached to it and can be created again on demand.
 * */
@Public
interface ChatThreadEventHandler {

    /**
     * Sends an [event] to server without further delays. If sending of the
     * event fails, the event is considered consumed anyway.
     *
     * If the event is sent to the server (not to be confused with processed
     * by the server), the [listener] is invoked.
     *
     * @param event [ChatThreadEvent] subclass which generates an event model.
     * @param listener nullable listener if client wants to know whether it
     * was dispatched
     * */
    fun trigger(event: ChatThreadEvent, listener: OnEventSentListener? = null)

    /**
     * Listener allowing to lister to event sent changes.
     * */
    @Public
    fun interface OnEventSentListener {
        /**
         * Notifies about event being sent to the server
         * */
        fun onSent()
    }

}
