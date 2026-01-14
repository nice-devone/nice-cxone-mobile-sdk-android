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

/**
 * Interface for handling chat conversation (thread) actions.
 * This interface provides methods to handle specific actions related to chat conversation (thread),
 * such as displaying inactivity popups.
 */
@Public
interface ChatThreadActionHandler : AutoCloseable {

    /**
     * Registers a listener for popup events.
     *
     * @param listener An implementation of the [OnPopup] interface that will handle popup events.
     */
    fun onPopup(listener: OnPopup)

    /**
     * Functional interface for handling popup events.
     */
    @Public
    fun interface OnPopup {
        /**
         * Request to show an inactivity popup.
         * This method is called when an popup was received as a new event or when the conversation was recovered and the
         * popup is the latest event in the conversation.
         *
         * @param popup An instance of [Popup] containing information about the popup
         *              that should be presented to the user.
         */
        fun onShowPopup(popup: Popup)
    }
}
