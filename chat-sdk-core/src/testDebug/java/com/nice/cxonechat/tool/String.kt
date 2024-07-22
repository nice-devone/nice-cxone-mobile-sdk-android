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

package com.nice.cxonechat.tool

private val Default = ('a'..'z') + ('A'..'Z') + ('0'..'9')

internal fun nextString(length: Int = 10, pool: List<Char> = Default) = buildString {
    repeat(length) {
        append(pool.random())
    }
}

internal fun nextStringPair(length: () -> Int = { 10 }, pool: List<Char> = Default): Pair<String, String> =
    nextString(length(), pool) to nextString(length(), pool)

internal fun nextStringMap(
    capacity: Int = 1,
    length: () -> Int = { 10 },
    pool: List<Char> = Default,
): Map<String, String> = buildMap(capacity = capacity) {
    put(nextString(length(), pool), nextString(length(), pool))
}
