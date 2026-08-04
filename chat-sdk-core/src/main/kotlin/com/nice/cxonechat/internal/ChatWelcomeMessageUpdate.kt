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

import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class ChatWelcomeMessageUpdate(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin, LoggerScope by LoggerScope("ChatWelcomeMessageUpdate", origin.entrails.logger) {

    private var listenerJob: Job? = null

    override suspend fun connect(): Unit = scope("connect") {
        listenerJob?.cancel()
        // Scope choices:
        //   Single collector — no supervisorScope needed; sibling isolation is irrelevant with
        //     one listener, and cross-decorator isolation is handled by the SupervisorJob at
        //     the root of entrails.threading.coroutineScope.
        //   CoroutineStart.UNDISPATCHED — subscription to the hot messageFlow completes
        //     synchronously before origin.connect() opens the socket, closing a theoretical
        //     missed-emission window (messageFlow has replay = 0).
        //   safeCollect — a throw inside the collector body is logged; collection continues so
        //     subsequent proactive-action events are not silently dropped.
        listenerJob = entrails.threading.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            socketListener.eventFlow(EventProactiveAction).safeCollect(this@ChatWelcomeMessageUpdate) { (_, model) ->
                if (model.type == ActionType.WelcomeMessage) {
                    storage.welcomeMessage = model.bodyText
                    val customFields = model.customFields.map(CustomFieldModel::toCustomField)
                    fields = (customFields + fields).distinctBy { it.id }
                }
            }
        }
        origin.connect()
    }

    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending() = scope("closeSuspending") {
        listenerJob?.cancel()
        listenerJob = null
        origin.closeSuspending()
    }
}
