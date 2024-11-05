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
import com.nice.cxonechat.internal.model.network.ActionExecuteTrigger
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.UUID

/**
 * Event that triggers event specified in agent console or elsewhere as per your
 * representative instructions. This event is not mandatory though, consult your
 * representative for more information.
 * */
@Public
internal class TriggerEvent(
    private val id: UUID,
) : ChatEvent<ActionExecuteTrigger>() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionExecuteTrigger(
        connection = connection,
        destination = storage.destinationId,
        visitor = storage.visitorId,
        id = id
    )
}
