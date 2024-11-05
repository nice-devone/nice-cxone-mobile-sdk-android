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

package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionDisplayed
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event provided by the client, notifying backend of a new activity performed
 * by user. Consult a representative if you need more information.
 *
 * Generally, this can be any form of data or serializable object.
 * */
@Public
internal class CustomVisitorEvent(
    private val data: Any,
) : ChatEvent<StoreVisitorEventsModel>() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = StoreVisitorEventsModel(
        connection = connection,
        visitor = storage.visitorId,
        destination = storage.destinationId,
        ProactiveActionDisplayed to data.toJsonElement(),
        createdAt = Date(),
    )

    private fun Any.toJsonElement(): JsonElement = when (this) {
        this::class.java.isAnnotationPresent(Serializable::class.java) -> Default.serializer.encodeToJsonElement(this)
        is String -> JsonPrimitive(this)
        else -> Default.serializer.encodeToJsonElement(this)
    }
}
