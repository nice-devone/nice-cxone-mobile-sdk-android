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

package com.nice.cxonechat

import kotlinx.coroutines.flow.Flow

/**
 * Handler allowing to listen to global popups whenever they are available. You are
 * encouraged to create this instance as soon as possible: immediately after being created it
 * starts listening to popup events and buffers only the latest pending one until a collector
 * consumes it. Delivery is single-consumer and consume-once — a buffered popup is delivered to the
 * first collector and is not broadcast or replayed to additional/later collectors.
 */
@Public
interface ChatActionHandler : AutoCloseable {

    /**
     * A [Flow] that emits [PopupEvent]s when popups should be displayed.
     * A popup that arrives before collection starts is delivered to the first collector; the flow
     * does not replay to later collectors, and each popup is delivered once (single-consumer).
     *
     * Example usage:
     * ```kotlin
     * val handler = chat.actions()
     * handler.popupFlow.collect { event ->
     *     showPopup(event.variables, event.metadata)
     * }
     * ```
     *
     * To stop listening, cancel the Flow collection or call [close].
     * @see close
     */
    val popupFlow: Flow<PopupEvent>

    /**
     * Removes listeners and releases internal server listener. After calling
     * this method, this instance is dead and its features are undefined in
     * that case.
     */
    override fun close()
}
