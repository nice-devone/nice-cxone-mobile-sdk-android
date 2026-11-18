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

package com.nice.cxonechat.sample.utilities

import android.content.Intent
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult
import com.nice.cxonechat.sample.data.models.parseStoreLaunchRequest

/**
 * Read the automated launch instructions, if any, carried by this intent.
 *
 * All parsing and validation lives in [parseStoreLaunchRequest]; this is only the adapter which
 * exposes the intent's extras as an [ExtrasSource].
 *
 * @return What the intent asked for.
 */
fun Intent.toStoreLaunchRequestResult(): StoreLaunchRequestResult = parseStoreLaunchRequest(IntentExtrasSource(this))

private class IntentExtrasSource(private val intent: Intent) : ExtrasSource {
    override fun has(key: String): Boolean = intent.hasExtra(key)

    override fun string(key: String): String? = intent.getStringExtra(key)

    override fun long(key: String): Long? {
        if (!intent.hasExtra(key)) return null
        // An extra stored as an int (`--ei`) doesn't satisfy getLongExtra, which then hands back the
        // default; the sentinel lets that be reported instead of read as a real value.
        return intent.getLongExtra(key, ABSENT).takeIf { it != ABSENT }
    }

    override fun boolean(key: String): Boolean? {
        if (!intent.hasExtra(key)) return null
        // Reading with both defaults distinguishes a real value from a wrongly-typed extra, which
        // would otherwise hand back whichever default was asked for.
        return intent.getBooleanExtra(key, false).takeIf { it == intent.getBooleanExtra(key, true) }
    }

    private companion object {
        const val ABSENT = Long.MIN_VALUE
    }
}
