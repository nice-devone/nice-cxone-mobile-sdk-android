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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.event.thread.ArchiveThreadEvent.eventId
import com.nice.cxonechat.internal.model.network.ActionArchiveThread
import com.nice.cxonechat.internal.socket.EventCallback.EventWithId
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

/**
 * Event that archives the thread it was invoked upon.
 *
 * Successful thread archivation will trigger thread list refresh.
 *
 * @property eventId Unique identifier for event.
 */
@Public
@Deprecated("Use ChatThreadHandler.archive() instead")
object ArchiveThreadEvent : ArchiveThreadEventImpl() {
    override val eventId: UUID
        get() = UUIDProvider.next()
}

/**
 * Event that archives the thread it was invoked upon.
 *
 * Successful thread archivation will trigger thread list refresh.
 *
 * @property eventId Unique identifier for event.
 */
open class ArchiveThreadEventImpl(
    override val eventId: UUID = UUIDProvider.next()
) : ChatThreadEvent(), EventWithId {
    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any = ActionArchiveThread(
        connection = connection,
        eventId = eventId,
        thread = thread
    )

    override fun toString(): String = "ArchiveThreadEvent()"
}
