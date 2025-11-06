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

import com.nice.cxonechat.internal.model.Visitor
import retrofit2.Callback

/**
 * The class which applies effect on supplied origin parameter.
 * The effect will trigger creation/update of [Visitor] on backend when this class is initialized.
 *
 * @param origin [ChatWithParameters] instance to which to apply the effect.
 * @param callback Callback which will be notified about result [Visitor] creation/update.
 */
internal class ChatStoreVisitor(
    private val origin: ChatWithParameters,
    private val callback: Callback<Void>,
) : ChatWithParameters by origin {

    private val retryApiHandler = RetryApiHandler(maxRetries = 2, retryIntervalMs = 30_000L)

    init {
        entrails.threading.background {
            sendVisitorInfo()
        }
    }

    private fun sendVisitorInfo() {
        val createOrUpdateVisitor = entrails.service.createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = entrails.storage.visitorId.toString(),
            visitor = Visitor(connection, origin.storage.deviceToken)
        )

        val params = createVisitorRetryParams(createOrUpdateVisitor, callback, chatStateListener)
        retryApiHandler.executeWithRetry(params.action, params.onSuccess, params.onFailure)
    }

    override fun close() {
        retryApiHandler.cancel()
        origin.close()
    }
}
