package com.nice.cxonechat.tool

import com.nice.cxonechat.Cancellable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration

internal inline fun <T> awaitResult(
    timeout: Duration? = null,
    body: (trigger: (T) -> Unit) -> Any,
): T {
    val latch = CountDownLatch(1)
    var result: T? = null
    val bodyResult = body {
        result = it
        latch.countDown()
    }
    try {
        when (timeout) {
            null -> latch.await()
            else -> latch.await(timeout.inWholeMilliseconds, MILLISECONDS)
        }
    } finally {
        if (bodyResult is Cancellable) bodyResult.cancel()
    }
    return result as T
}
