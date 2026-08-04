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

package com.nice.cxonechat.ui.util.preview.message

import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.domain.model.Person

/** Converts [MessageDirection] to a [Person] with a default ID and image URL. Only for preview purposes. */
internal fun MessageDirection.toPerson(id: String = "", imageUrl: String? = null) = Person(
    id = id,
    firstName = if (this === ToClient) "Agent" else "Customer",
    lastName = "Preview",
    imageUrl = imageUrl
)
