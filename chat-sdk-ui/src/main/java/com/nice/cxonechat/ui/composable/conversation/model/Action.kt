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

package com.nice.cxonechat.ui.composable.conversation.model

import com.nice.cxonechat.message.Media
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.message.Action as SdkAction
import com.nice.cxonechat.message.Action.ReplyButton as SdkReplyButton

internal sealed class Action {

    /**
     * Display a button which *may* have an associated icon.
     * If the button is selected integrating application should send [OutboundMessage]
     * with the supplied [text] and [postback].
     */
    data class ReplyButton(
        private val action: SdkReplyButton,
        private val sendMessage: (OutboundMessage) -> Unit,
    ) : Action() {
        /** Text to display on a button.  */
        val text: String = action.text

        /** Postback to be sent as part of the [OutboundMessage] if the button is selected. */
        val postback: String? = action.postback

        /** optional media/image to display with the media. */
        val media: Media? = action.media

        /** optional longer more descriptive text to display with the button. */
        val description: String? = action.description

        /**
         * Action to be invoked when action was activated.
         */
        val onClick: () -> Unit = {
            val postback = action.postback
            if (postback != null) {
                sendMessage(OutboundMessage(action.text, action.postback))
            }
        }
    }
}

internal fun SdkAction.toUiAction(sendMessage: (OutboundMessage) -> Unit): Action? = when (this) {
    is SdkReplyButton -> ReplyButton(this, sendMessage)
    else -> null
}
