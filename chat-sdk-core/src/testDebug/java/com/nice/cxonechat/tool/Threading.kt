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

import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.internal.ThreadingExecutor
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask

internal val Threading.Companion.Identity: ThreadingExecutor
    get() {
        val executor: ExecutorService = mockk {
            every { submit(any()) } answers {
                arg<Runnable>(0).run()
                FutureTask({}, Unit)
            }
        }
        return ThreadingExecutor(executor, executor)
    }
