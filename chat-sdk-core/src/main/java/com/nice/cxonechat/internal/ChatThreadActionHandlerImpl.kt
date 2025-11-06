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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.internal.model.network.EventMessageCreated
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThread
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class ChatThreadActionHandlerImpl(
    private val chat: ChatWithParameters,
    private val chatThread: ChatThread,
) : ChatThreadActionHandler {

    private var onInactivityPopupListener: ChatThreadActionHandler.OnPopup? = null
    private var latestInactivity: Popup.InactivityPopup? = null

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    private val inactivityCancellable = if (chat.chatMode === ChatMode.LiveChat) {
        Cancellable(
            chat.socketListener.addCallback(EventMessageCreated) { eventMessageCreated ->
                if (eventMessageCreated.inThread(chatThread)) {
                    val popup = eventMessageCreated.popup
                    if (popup is Popup.InactivityPopup) {
                        handlePopup(popup)
                    }
                }
            }
        )
    } else {
        Cancellable.noop
    }

    internal fun handlePopup(popup: Popup.InactivityPopup) {
        val listener = onInactivityPopupListener
        if (listener == null) {
            readWriteLock.writeLock().withLock {
                latestInactivity = popup
            }
            return
        }
        chat.entrails.threading.foreground {
            listener.onShowPopup(popup)
        }
        latestInactivity = null
    }

    override fun onPopup(listener: ChatThreadActionHandler.OnPopup) {
        onInactivityPopupListener = listener
        val latest: Popup.InactivityPopup? = readWriteLock.readLock().withLock(::latestInactivity)
        if (latest != null) {
            readWriteLock.writeLock().withLock {
                // We have to check again, because another thread could have set the latestInactivity to null
                if (latestInactivity != null) {
                    chat.entrails.threading.foreground {
                        listener.onShowPopup(latest)
                    }
                    latestInactivity = null
                }
            }
        }
    }

    override fun close() {
        inactivityCancellable.cancel()
        onInactivityPopupListener = null
    }
}
