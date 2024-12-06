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

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState

internal object CaseStatusChangedHandlerActions {
    inline fun handleCaseClosed(
        thread: ChatThreadMutable,
        event: EventCaseStatusChanged,
        crossinline onThreadUpdate: (ChatThread) -> Unit,
    ) {
        if (event.inThread(thread)) {
            val notArchived = event.status !== Closed
            val canAddMoreMessagesChanged = notArchived != thread.canAddMoreMessages
            if (canAddMoreMessagesChanged) {
                thread.update(
                    thread.asCopyable().copy(
                        canAddMoreMessages = notArchived,
                        threadState = if (!notArchived) ChatThreadState.Closed else thread.threadState
                    )
                )
                onThreadUpdate(thread)
            }
        }
    }
}
