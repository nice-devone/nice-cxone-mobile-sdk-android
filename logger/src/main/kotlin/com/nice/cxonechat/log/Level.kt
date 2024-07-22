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

package com.nice.cxonechat.log

/**
 * Logging level.
 */
sealed class Level : Comparable<Level> {

    /** Integer representation of a given level. */
    abstract val intValue: Int

    /** Compares itself to [other] level for convenience. */
    override operator fun compareTo(other: Level): Int = intValue.compareTo(other.intValue)

    /** Error level corresponds to integer value of 1000. */
    data object Error : Level() {
        override val intValue: Int
            get() = 1000
    }

    /** Warning level corresponds to integer value of 900. */
    data object Warning : Level() {
        override val intValue: Int
            get() = 900
    }

    /** Info level corresponds to integer value of 800. */
    data object Info : Level() {
        override val intValue: Int
            get() = 800
    }

    /** Debug level corresponds to integer value of 400. */
    data object Debug : Level() {
        override val intValue: Int
            get() = 400
    }

    /** Verbose level corresponds to integer value of 300. */
    data object Verbose : Level() {
        override val intValue: Int
            get() = 300
    }

    /**
     * All level corresponds to integer value of [Int.MIN_VALUE].
     * Note that this option may not be supported by all Logger
     * implementations.
     * */
    data object All : Level() {
        override val intValue: Int
            get() = Int.MIN_VALUE
    }

    /**
     * Custom level allows you to specify your own values and
     * store them statically.
     * */
    class Custom(
        override val intValue: Int,
    ) : Level()
}
