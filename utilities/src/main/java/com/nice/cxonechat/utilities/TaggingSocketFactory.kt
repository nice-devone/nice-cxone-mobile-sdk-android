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

package com.nice.cxonechat.utilities

import android.net.TrafficStats
import android.os.Build
import android.system.Os
import java.net.Socket

/**
 * SocketFactory to tag sockets per [TrafficStats].
 *
 * All created sockets will be tagged with the current thread id, and if
 * running on an SDK >= P, with the process UID.
 */
object TaggingSocketFactory : DelegatingSocketFactory() {
    override fun configure(socket: Socket) {
        val socketTag = Thread.currentThread().id.toInt()
        if (TrafficStats.getThreadStatsTag() != socketTag) {
            TrafficStats.setThreadStatsTag(socketTag)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TrafficStats.setThreadStatsUid(Os.getuid())
        }
        TrafficStats.tagSocket(socket)
    }
}
