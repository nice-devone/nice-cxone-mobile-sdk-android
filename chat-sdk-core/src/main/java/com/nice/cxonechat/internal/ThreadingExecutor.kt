package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Cancellable.Companion.asCancellable
import java.util.concurrent.ExecutorService

internal class ThreadingExecutor(
    private val background: ExecutorService,
    private val foreground: ExecutorService,
) : Threading {

    override fun foreground(runnable: Runnable): Cancellable {
        return foreground.submit(runnable).asCancellable()
    }

    override fun background(runnable: Runnable): Cancellable {
        return background.submit(runnable).asCancellable()
    }

}
