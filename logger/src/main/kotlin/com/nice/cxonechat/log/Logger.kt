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
 * Abstraction over logging implementation. The implementation is agnostic
 * but should be pluggable to almost any framework out there.
 * */
interface Logger {

    /**
     * Uses [level] to determine whether it should be passed to the underlying
     * implementation. Implementations are free to use whichever level they like.
     *
     * @param level Used to determine logging level. Platform levels are strongly
     * encouraged
     * @param message Message supplied to the implementation. Note that
     * implementations might have limits on message length.
     * @param throwable Error that occurred or null if the message only carries
     * information
     * */
    fun log(level: Level, message: String, throwable: Throwable? = null)
}
