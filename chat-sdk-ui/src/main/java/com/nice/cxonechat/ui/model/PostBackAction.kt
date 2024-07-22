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

package com.nice.cxonechat.ui.model

import android.view.View
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.message.OutboundMessage

/**
 * Action which will send a new chat message with supplied [postback] as the event's value.
 */
internal class PostBackAction(
    private val handler: ChatThreadMessageHandler,
    private val postback: String,
    private val message: String,
) : OnViewAction {
    override fun invoke(view: View) = handler.send(OutboundMessage(message, postback))
}
