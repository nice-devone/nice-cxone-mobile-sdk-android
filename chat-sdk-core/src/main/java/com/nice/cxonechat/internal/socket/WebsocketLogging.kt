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

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import okhttp3.WebSocket

/**
 * [LoggerScope] for [WebSocket], which logs text of messages sent via [send].
 * Also logs calls to [close].
 *
 * @param socket The wrapped [WebSocket].
 * @param logger Base for the [LoggerScope] used by this implementation.
 */
internal class WebsocketLogging(
    private val socket: WebSocket,
    logger: Logger,
) : WebSocket by socket, LoggerScope by LoggerScope<WebSocket>(logger) {
    override fun send(text: String): Boolean = scope("send") {
        verbose(text)
        socket.send(text)
    }

    override fun close(code: Int, reason: String?): Boolean = scope("close") {
        socket.close(code, reason)
    }
}
