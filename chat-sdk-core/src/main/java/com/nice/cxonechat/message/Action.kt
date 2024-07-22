/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Actionable item to display as part of a message.
 */
@Public
interface Action {
    /**
     * Display a button which *may* have an associated icon.
     * If the button is selected integrating application should send [OutboundMessage]
     * with the supplied [text] and [postback].
     */
    @Public
    interface ReplyButton : Action {
        /** Text to display on a button.  */
        val text: String

        /** Postback to be sent as part of the [OutboundMessage] if the button is selected. */
        val postback: String?

        /** optional media/image to display with the media. */
        val media: Media?

        /** optional longer more descriptive text to display with the button. */
        val description: String?
    }
}
