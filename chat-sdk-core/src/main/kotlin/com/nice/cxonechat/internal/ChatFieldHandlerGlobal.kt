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
import com.nice.cxonechat.event.SetCustomerCustomFieldEvent
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.CustomFieldInternal.Companion.updateWith
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope

internal class ChatFieldHandlerGlobal(
    private val chat: ChatWithParameters,
    parentScope: LoggerScope,
) : ChatFieldHandler, LoggerScope by LoggerScope("ChatFieldHandlerGlobal", parentScope) {

    override fun add(fields: Map<String, String>): Unit = scope("add") {
        chat.fields = chat.fields.updateWith(
            fields.map(::CustomFieldInternal)
        )
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerSetCustomerCustomField")) {
            chat.events().trigger(SetCustomerCustomFieldEvent(fields))
        }
    }
}
