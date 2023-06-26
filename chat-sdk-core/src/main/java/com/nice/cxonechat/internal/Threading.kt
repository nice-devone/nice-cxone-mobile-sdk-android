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
