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

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose

internal class ChatActionHandlerLogging(
    private val origin: ChatActionHandler,
    logger: Logger,
) : ChatActionHandler, LoggerScope by LoggerScope<ChatActionHandler>(logger) {

    init {
        verbose("Initialized")
    }

    override fun onPopup(listener: OnPopupActionListener) = scope("onPopup") {
        verbose("Registered")
        origin.onPopup { params, metadata ->
            scope("onShowPopup") {
                verbose("params=$params, metadata=$metadata")
                duration { listener.onShowPopup(params, metadata) }
            }
        }
    }

    override fun close() = scope("close") {
        duration { origin.close() }
    }
}
