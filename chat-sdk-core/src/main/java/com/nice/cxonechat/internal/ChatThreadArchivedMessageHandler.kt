/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.message.OutboundMessage

internal class ChatThreadArchivedMessageHandler(
    private val origin: ChatThreadMessageHandler,
    private val chatThreadHandlerImpl: ChatThreadHandler,
) : ChatThreadMessageHandler by origin {

    override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
        val chatThread = chatThreadHandlerImpl.get()
        check(chatThread.canAddMoreMessages) { "Trying to send message to an archived thread with id: ${chatThread.id}" }
        origin.send(message, listener)
    }
}
