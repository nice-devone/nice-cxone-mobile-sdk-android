/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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
        val closes = event.status === Closed
        if (!event.inThread(thread) || !closes && thread.staysClosedFor(event.contactId)) return
        val canAddMoreMessagesChanged = closes == thread.canAddMoreMessages
        // archive() disables sending but leaves the state, so a later Closed must still close the thread.
        val closesThread = closes && thread.threadState !== ChatThreadState.Closed
        // A closed thread still records which case closed, so that case's late events keep it closed.
        val learnsCase = closes && event.contactId != null && event.contactId != thread.contactId
        if (!canAddMoreMessagesChanged && !closesThread && !learnsCase) return
        thread.update(
            if (closes) {
                thread.asCopyable().copy(
                    canAddMoreMessages = false,
                    threadState = ChatThreadState.Closed,
                    contactId = event.contactId ?: thread.contactId,
                    contactHandler = event.contactHandler ?: thread.contactHandler,
                )
            } else {
                thread.asCopyable().copy(
                    canAddMoreMessages = true,
                    contactHandler = event.contactHandler ?: thread.contactHandler,
                )
            }
        )
        onThreadUpdate(thread)
    }

    /**
     * A closed thread ignores the status of its own case: only an event for a different, known case reopens it.
     */
    fun ChatThread.staysClosedFor(eventContactId: String?): Boolean =
        threadState === ChatThreadState.Closed &&
            (contactId == null || eventContactId == null || contactId == eventContactId)
}
