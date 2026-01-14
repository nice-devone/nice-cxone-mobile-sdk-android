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

package com.nice.cxonechat.ui.composable.conversation.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.ui.util.DateProvider
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker
import com.nice.cxonechat.ui.util.preview.message.UiSdkMetadata
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import com.nice.cxonechat.ui.util.preview.message.asMessage
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

@Suppress("LongParameterList")
internal class PreviewMessageProvider : PreviewParameterProvider<Message> {
    val messageinvocation = AtomicInteger(0)
    val messages: Sequence<SdkMessage>
        get() = sequenceOf(
            UiSdkText("Text 1", createdAt = DateProvider.now()),
            UiSdkListPicker(createdAt = DateProvider.now()),
            UiSdkText(
                text = MESSAGE.format(messageinvocation.andIncrement),
                direction = ToAgent,
                metadata = UiSdkMetadata(status = MessageStatus.Delivered),
                createdAt = DateProvider.now()
            ),
            UiSdkText(
                text = MESSAGE.format(messageinvocation.andIncrement),
                direction = ToAgent,
                metadata = UiSdkMetadata(status = MessageStatus.Delivered),
                createdAt = DateProvider.now()
            ),
            UiSdkText(
                text = MESSAGE.format(messageinvocation.andIncrement),
                direction = ToAgent,
                metadata = UiSdkMetadata(status = MessageStatus.Delivered),
                createdAt = DateProvider.now()
            ),
            UiSdkText(
                text = MESSAGE.format(messageinvocation.andIncrement),
                direction = ToAgent,
                metadata = UiSdkMetadata(status = MessageStatus.Seen),
                createdAt = DateProvider.now()
            ),
            UiSdkText(
                text = MESSAGE.format(messageinvocation.andIncrement),
                direction = ToAgent,
                metadata = UiSdkMetadata(status = MessageStatus.FailedToDeliver),
                createdAt = DateProvider.now()
            ),
            UiSdkQuickReply(createdAt = DateProvider.now()),
            UiSdkRichLink(createdAt = DateProvider.now()),
        )

    override val values = messages.map { it.asMessage() }

    companion object {
        private const val MESSAGE = "Message %d"
        private var nextDate = Date().time

        init {
            DateProvider.now = {
                Date(nextDate.also { nextDate += 1 })
            }
        }
    }
}
