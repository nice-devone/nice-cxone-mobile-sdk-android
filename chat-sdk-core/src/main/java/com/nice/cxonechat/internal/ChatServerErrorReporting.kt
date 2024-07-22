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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.enums.ErrorType.ArchivingThreadFailed
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.ErrorType.SendingMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOfflineMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOutboundFailed
import com.nice.cxonechat.enums.ErrorType.SendingTranscriptFailed
import com.nice.cxonechat.enums.ErrorType.UpdatingThreadFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener

/**
 * Class registers callbacks for error events which can't be directly associated with any action, either because of the
 * nature of the error or because of the missing metadata in the error and asynchronous nature of the actions.
 *
 * The need for the callbacks can be in the future eliminated either by adding metadata to the error or by preventing
 * parallel execution of (some) actions.
 *
 * @param origin Wrapped original [ChatWithParameters].
 */
internal class ChatServerErrorReporting(private val origin: ChatWithParameters) : ChatWithParameters by origin {

    private val callbacks = Cancellable(
        socketListener.addErrorCallback(SendingMessageFailed),
        socketListener.addErrorCallback(RecoveringThreadFailed),
        socketListener.addErrorCallback(SendingOutboundFailed),
        socketListener.addErrorCallback(UpdatingThreadFailed),
        socketListener.addErrorCallback(ArchivingThreadFailed),
        socketListener.addErrorCallback(SendingTranscriptFailed),
        socketListener.addErrorCallback(SendingOfflineMessageFailed),
    )

    override fun close() {
        callbacks.cancel()
        origin.close()
    }

    private fun ProxyWebSocketListener.addErrorCallback(type: ErrorType): Cancellable = addErrorCallback(type) {
        chatStateListener?.onChatRuntimeException(ServerCommunicationError(type.value))
    }
}
