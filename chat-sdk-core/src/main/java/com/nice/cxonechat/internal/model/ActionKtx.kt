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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.event.thread.PostbackEvent
import com.nice.cxonechat.event.thread.ReplyButtonEvent
import com.nice.cxonechat.message.Action

internal object ActionKtx {
    internal fun Action.toEvent(): ChatThreadEvent? =
        when (this) {
            is ActionInternal.PostbackReplyButton -> postback?.let { actionPostback ->
                PostbackEvent(
                    text = text,
                    postback = actionPostback,
                )
            }

            is ActionInternal.ReplyButton -> postback?.let { actionPostback ->
                ReplyButtonEvent(this)
            }

            else -> null
        }
}
