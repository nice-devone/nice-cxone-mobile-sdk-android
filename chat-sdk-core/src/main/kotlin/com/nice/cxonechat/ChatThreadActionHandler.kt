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
 * Interface for handling chat conversation (thread) actions.
 * This interface provides methods to handle specific actions related to chat conversation (thread),
 * such as displaying inactivity popups.
 *
 * There is no close or release step: collect [popupFlow] for as long as popups should be shown and
 * cancel the collection to stop. This type does not implement `AutoCloseable` — the old
 * listener-plus-`close()` model was replaced by the cold [popupFlow].
 */
@Public
interface ChatThreadActionHandler {

    /**
     * A [Flow] that emits [Popup] events when inactivity popups should be displayed.
     *
     * This flow emits popups received as new events or when the conversation was recovered
     * and the popup is the latest event in the conversation. It is a cold flow: each collection
     * establishes its own subscription, so collecting again after a reconnect resumes delivery.
     * Collect it for the whole time popups should be observed; popups delivered while no collector
     * is active are not retained.
     *
     * To stop listening, cancel the Flow collection — the flow is cold and holds no resources.
     */
    val popupFlow: Flow<Popup>
}
