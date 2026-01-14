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

import com.nice.cxonechat.message.Action
import java.util.Date

/**
 * Represents a popup interface used in the chat system.
 * This interface defines the common structure and behavior of the popups that can be displayed to users.
 */
@Public
interface Popup {
    /**
     * An popup that is used to display an inactivity prompt to user in the chat.
     * This popup can be sent by the automation on server when the user has been inactive for a certain period of time.
     *
     * In some cases it can also be sent by the agent to trigger user response or to end the conversation.
     * It contains a countdown timer and buttons to either refresh the chat or expire the current conversation.
     */
    @Public
    interface InactivityPopup : Popup {
        /**
         * The title of the inactivity popup message.
         */
        val title: String

        /**
         * The main text of the inactivity popup message, it should explain the reason for the popup.
         */
        val body: String

        /**
         * Parameters for the countdown timer of the inactivity popup.
         */
        val countdown: Countdown

        /**
         * Additional text which describes what the user should do next.
         */
        val callToAction: String

        /**
         * Action that will refresh the chat and reset the inactivity timer.
         */
        val sessionRefresh: Action

        /**
         * Action that will expire the current conversation and end the chat session.
         */
        val sessionExpire: Action

        /**
         * Parameters for the countdown timer.
         */
        @Public
        interface Countdown {
            /**
             * The time after which the countdown will finish (even when the application is in background).
             */
            val timeoutSeconds: Long

            /**
             * The time when the countdown started.
             */
            val startedAt: Date
        }
    }
}
