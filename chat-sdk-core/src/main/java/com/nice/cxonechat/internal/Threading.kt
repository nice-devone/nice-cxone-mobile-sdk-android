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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal interface Threading {

    fun foreground(runnable: Runnable): Cancellable
    fun background(runnable: Runnable): Cancellable

    companion object {

        @JvmName("getDefault")
        operator fun invoke(foreground: ExecutorService): Threading {
            return ThreadingExecutor(
                background = Executors.newCachedThreadPool(),
                foreground = foreground
            )
        }
    }
}
