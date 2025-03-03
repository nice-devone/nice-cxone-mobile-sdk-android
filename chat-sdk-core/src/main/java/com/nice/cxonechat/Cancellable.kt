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

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {

        internal val noop: Cancellable = NOOP

        /** No operation singleton implementation of [Cancellable]. */
        internal data object NOOP : Cancellable {
            override fun cancel() = Unit
        }

        @CheckResult
        internal fun Future<*>.asCancellable() = Cancellable {
            cancel(true)
        }

        @CheckResult
        internal operator fun invoke(vararg cancellables: Cancellable?) = Cancellable {
            cancellables.asIterable().cancel()
        }

        internal fun Iterable<Cancellable?>.cancel() {
            filterNotNull().forEach(Cancellable::cancel)
        }
    }
}
