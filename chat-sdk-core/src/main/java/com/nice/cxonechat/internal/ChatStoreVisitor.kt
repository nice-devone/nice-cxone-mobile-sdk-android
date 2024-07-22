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
    origin: ChatWithParameters,
    callback: Callback<Void>,
) : ChatWithParameters by origin {
    init {
        val createOrUpdateVisitor = entrails.service.createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = entrails.storage.visitorId.toString(),
            visitor = Visitor(connection, origin.storage.deviceToken)
        )
        runCatching {
            callback.onResponse(createOrUpdateVisitor, createOrUpdateVisitor.execute())
        }.onFailure {
            callback.onFailure(createOrUpdateVisitor, it)
        }
    }
}
