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

/**
 * Read-only access to a set of named values, abstracting away [android.content.Intent] extras.
 *
 * Exists so that launch-request parsing and validation can be exercised by plain JVM unit tests,
 * which cannot construct an [android.content.Intent].
 */
interface ExtrasSource {
    /**
     * True if anything at all is stored under [key], whatever its type.
     *
     * The typed readers below cannot distinguish "absent" from "supplied with the wrong type", so
     * this is what lets a mistyped extra be reported instead of silently ignored.
     */
    fun has(key: String): Boolean

    /**
     * Value stored under [key], or null when it is absent or not a string.
     */
    fun string(key: String): String?

    /**
     * Value stored under [key], or null when it is absent or not a long.
     *
     * Returning null for a present-but-wrong-typed value is deliberate: `adb shell am start --ei`
     * stores an int, and silently reading that as a default `0` brand id would connect the sample
     * app to the wrong place instead of reporting the mistake.
     */
    fun long(key: String): Long?

    /**
     * Value stored under [key], or null when it is absent or not a boolean.
     *
     * Nullable rather than defaulted so that callers can tell "not supplied" from "supplied as
     * false" -- an extra which defaults to true needs that distinction.
     */
    fun boolean(key: String): Boolean?
}
