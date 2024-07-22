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

package com.nice.cxonechat.sample.utilities

import android.content.Context
import android.util.Patterns
import com.nice.cxonechat.sample.R

/**
 * A requirement to be placed upon a string.
 *
 * When the requirement is invoked, it should return null if there is no
 * error or an appropriate error message if the string is in error.
 */
typealias Requirement = (Context, String) -> String?

/**
 * Standard predefined requirements for strings.
 */
object Requirements {
    /** no requirements, anything is acceptable. */
    val none: Requirement = { _, _ -> null }

    /** The string must be non-blank. */
    val required: Requirement = { context, text ->
        if (text.isBlank()) {
            context.getString(R.string.error_required_field)
        } else {
            null
        }
    }

    /** The string must be a valid floating point (Double) number. */
    val floating: Requirement = { context, text ->
        if (text.toDoubleOrNull() == null) {
            context.getString(R.string.error_invalid_number)
        } else {
            null
        }
    }

    /** The string must be a valid integer (Long) number. */
    val integer: Requirement = { context, text ->
        if (text.toLongOrNull() == null) {
            context.getString(R.string.error_invalid_integer)
        } else {
            null
        }
    }

    /** The string must be a validly formatted email address. */
    val email: Requirement = { context, text ->
        if (Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            null
        } else {
            context.getString(R.string.error_email_validation)
        }
    }

    /**
     * Concatenate one or more requirements.
     *
     * The string is considered valid only if *all* the listed requirements are met.
     *
     * @param requirements [Requirement]s to be concatenated.
     */
    fun allOf(vararg requirements: Requirement): Requirement = { context, string ->
        requirements.fold(null as String?) { error, requirement ->
            error ?: requirement(context, string)
        }
    }
}
