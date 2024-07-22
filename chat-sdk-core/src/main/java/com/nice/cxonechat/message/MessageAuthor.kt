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

package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Author of a given [Message]. This field is backed by, or rather converted from,
 * different implementations depending on the [Message.direction] or possibly
 * other factors.
 *
 * If the message doesn't provide an actor(author), default values will be used.
 * These default values are thereafter the identical for the lifetime of the
 * process. Do not rely on the stability of IDs for an example after the process
 * finishes.
 */
@Public
abstract class MessageAuthor {
    /**
     * The id of an author. Converted implementations can have different implementations
     * for ids, though they are all converted to string for convenience.
     */
    abstract val id: String

    /**
     * First name of the given actor.
     *
     * @see name
     */
    abstract val firstName: String

    /**
     * Last name of the given actor.
     *
     * @see name
     */
    abstract val lastName: String

    /**
     * Optional URI with avatar image of message author.
     */
    abstract val imageUrl: String?

    /**
     * Merges [firstName] and [lastName] in this order, separated by a space.
     * If both values are empty, then returns an empty string.
     */
    val name
        get() = "$firstName $lastName".trim()
}
