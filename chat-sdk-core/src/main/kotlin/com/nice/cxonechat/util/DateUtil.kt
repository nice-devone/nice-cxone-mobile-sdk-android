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

package com.nice.cxonechat.util

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** Parses an ISO 8601 string to an [Instant], handling the legacy lowercase-z UTC suffix. */
internal fun String.toInstant(): Instant =
    Instant.parse(if (endsWith("z")) dropLast(1) + "Z" else this)

/** Serialises an [Instant] to an ISO 8601 string truncated to millisecond precision. */
internal fun Instant.toTimestamp(): String =
    Instant.fromEpochMilliseconds(toEpochMilliseconds()).toString()

/**
 * Truncates this instant to whole seconds, discarding the sub-second (nanosecond) component.
 *
 * The result is floored toward the past, so it never represents a moment later than the original.
 */
internal fun Instant.truncateToSeconds(): Instant =
    Instant.fromEpochSeconds(epochSeconds)

/** Returns `true` if this instant will expire within [duration] from now. */
internal fun Instant.expiresWithin(duration: Duration): Boolean =
    Clock.System.now() + duration > this
