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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.enums.ErrorType.ArchivingThreadFailed
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.ErrorType.SendingMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOfflineMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOutboundFailed
import com.nice.cxonechat.enums.ErrorType.UpdatingThreadFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.errorFlow
import com.nice.cxonechat.log.LoggerScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

/**
 * Class registers callbacks for error events which can't be directly associated with any action, either because of the
 * nature of the error or because of the missing metadata in the error and asynchronous nature of the actions.
 *
 * The need for the callbacks can be in the future eliminated either by adding metadata to the error or by preventing
 * parallel execution of (some) actions.
 *
 * @param origin Wrapped original [ChatWithParameters].
 */
internal class ChatServerErrorReporting(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin,
    LoggerScope by LoggerScope<Chat>(origin.entrails.logger) {

    private var callbacksJob: Job? = null

    override suspend fun connect() {
        callbacksJob?.cancel()
        // Scope choices:
        //   supervisorScope — intra-decorator isolation; a throw in one collector must not
        //     cancel siblings. Cross-decorator isolation is handled by the SupervisorJob at
        //     the root of entrails.threading.coroutineScope.
        //   CoroutineStart.UNDISPATCHED on each inner launch — subscriptions to the hot
        //     messageFlow complete synchronously before origin.connect() opens the socket,
        //     closing a theoretical missed-emission window (messageFlow has replay = 0).
        //   safeCollect — a throw inside a collector body is logged; collection continues so
        //     subsequent errors on that topic are still reported.
        callbacksJob = entrails.threading.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            supervisorScope {
                listOf(
                    SendingMessageFailed,
                    RecoveringThreadFailed,
                    SendingOutboundFailed,
                    UpdatingThreadFailed,
                    ArchivingThreadFailed,
                    SendingOfflineMessageFailed,
                ).forEach { errorType ->
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        socketListener.errorFlow(errorType).safeCollect(this@ChatServerErrorReporting) {
                            chatStateListener?.onChatRuntimeException(ServerCommunicationError(errorType.value))
                        }
                    }
                }
            }
        }
        origin.connect()
    }

    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending() {
        callbacksJob?.cancel()
        callbacksJob = null
        origin.closeSuspending()
    }
}
