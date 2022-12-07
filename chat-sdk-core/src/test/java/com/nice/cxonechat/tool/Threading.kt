package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.internal.ThreadingExecutor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask

internal val Threading.Companion.Identity: ThreadingExecutor
    get() {
        val executor: ExecutorService = mock()
        whenever(executor.submit(any())).then {
            val runnable = it.getArgument<Runnable>(0)
            runnable.run()
            FutureTask({}, Unit)
        }
        return ThreadingExecutor(executor, executor)
    }
