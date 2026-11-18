/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample.viewModel

import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicReference

/**
 * Hands a launch request from the activity which received the intent to the [StoreViewModel] which
 * acts on it.
 *
 * [StoreActivity][com.nice.cxonechat.sample.StoreActivity] can't call the ViewModel directly on a
 * cold start: the ViewModel is created lazily on first access, so calling it from `onCreate` would
 * race its own initialisation, which decides between the configuration dialog and a stored
 * configuration. Depositing the request here before that first access is instead strictly ordered,
 * and the ViewModel picks it up at exactly the point where it makes that decision.
 */
@Single
class PendingLaunchConfiguration {
    private val pending = AtomicReference<StoreLaunchRequest?>(null)

    /**
     * True while a request is waiting to be consumed.
     *
     * Read when the ViewModel is constructed, which is after the activity has deposited anything the
     * launch intent carried, so the storefront can be suppressed from the very first composition
     * rather than after the ViewModel's asynchronous initialisation has caught up.
     */
    val isPending: Boolean
        get() = pending.get() != null

    /**
     * Deposit [request] for the ViewModel to pick up, replacing any request not yet consumed.
     */
    fun offer(request: StoreLaunchRequest) {
        pending.set(request)
    }

    /**
     * Take the deposited request, if there is one. Subsequent calls return null until another
     * request is offered, so a request is never applied twice.
     */
    fun consume(): StoreLaunchRequest? = pending.getAndSet(null)
}
