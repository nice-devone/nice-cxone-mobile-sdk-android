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

import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

internal class ChatWelcomeMessageUpdate(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin {

    private val listener = socketListener.addCallback(EventProactiveAction) { model ->
        if (model.type == ActionType.WelcomeMessage) {
            storage.welcomeMessage = model.bodyText
            val customFields = model.customFields.map(CustomFieldModel::toCustomField)
            fields = (customFields + fields).distinctBy { it.id }
        }
    }

    override fun close() {
        listener.cancel()
        origin.close()
    }
}
