/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.api

import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.CXoneException

/**
 * Functional interface for error callbacks, providing Java 8+ lambda support.
 *
 * This interface is used by helper classes to provide a consistent
 * callback pattern for handling errors that occur during asynchronous
 * operations.
 */
@Public
@FunctionalInterface
fun interface ErrorCallback {
    /**
     * Called when an error occurs during asynchronous operation.
     *
     * @param exception The exception that occurred.
     */
    fun onError(exception: CXoneException)
}
