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

import com.nice.cxonechat.ChatEventHandler

/**
 * Interface for [ChatEventHandler] dependency injection.
 */
internal fun interface ChatEventHandlerProvider {
    fun events(chat: ChatWithParameters): ChatEventHandler

    companion object {
        /**
         * Default implementation of [ChatEventHandlerProvider] which supplies functional [ChatEventHandler] instance.
         */
        operator fun invoke() = ChatEventHandlerProvider { chat ->
            var handler: ChatEventHandler
            handler = ChatEventHandlerImpl(chat)
            handler = ChatEventHandlerTokenGuard(handler, chat)
            handler = ChatEventHandlerVisitGuard(handler, chat)
            handler = ChatEventHandlerTimeOnPage(handler, chat)
            handler = ChatEventHandlerThreading(handler, chat)
            handler
        }
    }
}
