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
