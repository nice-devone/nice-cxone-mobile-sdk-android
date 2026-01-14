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

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.event.thread.SetContactCustomFieldEvent
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.thread.ChatThreadState.Closed
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready
import com.nice.cxonechat.thread.ChatThreadState.Received

internal class ChatFieldHandlerThread(
    private val handler: ChatThreadHandler,
    private val thread: ChatThreadMutable,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        val customFields = fields.map(::CustomFieldModel)
        when (thread.threadState) {
            Received, Loaded, Ready -> handler.events().trigger(
                event = SetContactCustomFieldEvent(customFields),
                listener = { updateContactCustomFields(customFields) },
                errorListener = null,
            )

            Pending -> updateContactCustomFields(customFields)
            Closed -> throw InvalidStateException("Cannot add custom fields to a closed thread")
        }
    }

    private fun updateContactCustomFields(customFields: List<CustomFieldModel>) {
        val mappedFields = customFields
            .map(CustomFieldModel::toCustomField)
        thread += thread.asCopyable().copy(fields = mappedFields)
    }
}
