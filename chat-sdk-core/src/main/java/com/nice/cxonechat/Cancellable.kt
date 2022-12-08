package com.nice.cxonechat

import androidx.annotation.CheckResult
import java.util.concurrent.Future

@Public
fun interface Cancellable {

    fun cancel()

    companion object {

        internal val noop = Cancellable {}

        @CheckResult
        internal fun Future<*>.asCancellable() = Cancellable {
            cancel(true)
        }

        @CheckResult
        internal operator fun invoke(vararg cancellables: Cancellable) = Cancellable {
            for (cancellable in cancellables)
                cancellable.cancel()
        }
    }
}
