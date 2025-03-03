/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope

internal class ChatWelcomeMessageUpdate(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin, LoggerScope by LoggerScope("ChatWelcomeMessageUpdate", origin.entrails.logger) {

    private var listener: Cancellable = Cancellable.noop

    private fun prepareListener(): Cancellable = scope("prepareListener") {
        socketListener.addCallback(EventProactiveAction) { model ->
            if (model.type == ActionType.WelcomeMessage) {
                storage.welcomeMessage = model.bodyText
                val customFields = model.customFields.map(CustomFieldModel::toCustomField)
                fields = (customFields + fields).distinctBy { it.id }
            }
        }
        }

    private fun cancelListener() = scope("cancelListener") {
        listener.cancel()
    }

    override fun connect(): Cancellable = scope("connect") {
        origin.connect().also {
            cancelListener()
            listener = prepareListener()
        }
    }

    override fun close() = scope("close") {
        cancelListener()
        origin.close()
    }
}
