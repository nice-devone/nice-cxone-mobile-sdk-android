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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import okhttp3.WebSocket

@Suppress(
    "ComplexInterface"
)
internal interface ChatWithParameters : Chat {

    val entrails: ChatEntrails
    override val configuration: ConfigurationInternal
    val socket: WebSocket?
    val socketListener: ProxyWebSocketListener
    var connection: Connection
    override var fields: List<CustomField>

    /** Last page view event received, if any. */
    var lastPageViewed: PageViewEvent?

    val chatStateListener: ChatStateListener?

    override var isChatAvailable: Boolean

    val storage get() = entrails.storage
    val service get() = entrails.service
}
