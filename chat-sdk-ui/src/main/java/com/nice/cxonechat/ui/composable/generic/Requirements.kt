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

package com.nice.cxonechat.ui.composable.generic

import android.content.Context
import android.util.Patterns
import com.nice.cxonechat.ui.R.string

internal typealias Requirement = (Context, String) -> String?

internal object Requirements {
    val none: Requirement = { _, _ -> null }

    val required: Requirement = { context, text ->
        if (text.isBlank()) {
            context.getString(string.error_required_field)
        } else {
            null
        }
    }

    val floating: Requirement = { context, text ->
        if (text.toDoubleOrNull() == null) {
            context.getString(string.error_invalid_number)
        } else {
            null
        }
    }

    val integer: Requirement = { context, text ->
        if (text.toLongOrNull() == null) {
            context.getString(string.error_invalid_integer)
        } else {
            null
        }
    }

    val email: Requirement = { context, text ->
        if (Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            null
        } else {
            context.getString(string.error_email_validation)
        }
    }

    fun allOf(vararg requirements: Requirement): Requirement = { context, string ->
        requirements.fold(null as String?) { error, requirement ->
            error ?: requirement(context, string)
        }
    }
}
