/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionSetContactCustomFields
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

internal class SetContactCustomFieldEvent(
    private val fields: List<CustomFieldModel>,
) : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionSetContactCustomFields(
        connection = connection,
        thread = thread,
        fields = fields
    )
}
