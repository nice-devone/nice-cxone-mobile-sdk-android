/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.event.thread.SetContactCustomFieldEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.state.validate

internal class ChatFieldHandlerThread(
    private val chat: Chat,
    private val handler: ChatThreadHandler,
    private val thread: ChatThreadMutable,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        chat.configuration.allCustomFields.validate(fields)
        val customFields = fields.map(::CustomFieldModel)
        handler.events().trigger(
            event = SetContactCustomFieldEvent(customFields),
            listener = {
                val mappedFields = customFields
                    .map(CustomFieldModel::toCustomField)
                thread += thread.asCopyable().copy(fields = mappedFields)
            },
            errorListener = null,
        )
    }
}
