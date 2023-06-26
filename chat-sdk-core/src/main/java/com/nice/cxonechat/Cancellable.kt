package com.nice.cxonechat

import androidx.annotation.CheckResult
import java.util.concurrent.Future

/**
 * Interface used to provide applications a way how to cancel long-running background tasks.
 */
@Public
fun interface Cancellable {

    /**
     * Cancels the running operation, represented by the instance of this object.
     */
    fun cancel()

    companion object {

        internal val noop = Cancellable {}

        @CheckResult
        internal fun Future<*>.asCancellable() = Cancellable {
            cancel(true)
        }

        @CheckResult
        internal operator fun invoke(vararg cancellables: Cancellable) = Cancellable {
            for (cancellable in cancellables) {
                cancellable.cancel()
            }
        }
    }
}
