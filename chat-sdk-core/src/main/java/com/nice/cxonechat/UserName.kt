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

package com.nice.cxonechat

/**
 * User details as saved to file.
 */
@Public
interface UserName {
    /** Users last name. */
    val lastName: String

    /** Users first name. */
    val firstName: String

    /** Culturally insensitive version of customers full name. */
    val fullName: String
        get() = "$firstName $lastName"

    /** true iff the name is valid, a valid name requires both first and last names be non-blank. */
    val valid: Boolean
        get () = lastName.isNotBlank() && firstName.isNotBlank()

    @Public
    companion object {
        /** No UserName has been assigned. */
        val Anonymous = UserName("", "")

        /**
         * Create a default instance with the indicated first and last names.
         *
         * @param lastName Users last name.
         * @param firstName Users first name.
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(lastName: String, firstName: String): UserName = UserNameImpl(lastName, firstName)
    }
}

/**
 * User details as saved to file.
 *
 * @property lastName Users last name.
 * @property firstName Users first name.
 */
private data class UserNameImpl(
    override val lastName: String,
    override val firstName: String,
) : UserName
