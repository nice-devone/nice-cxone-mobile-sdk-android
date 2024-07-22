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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.runtime.Stable
import com.nice.cxonechat.ui.composable.conversation.ContentType.Attachment
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments

@Stable
internal val Message.contentType: ContentType
    get() = when (this) {
        is WithAttachments -> Attachment
        is Message.Text -> ContentType.Text
        is Message.ListPicker -> ContentType.ListPicker
        is Message.RichLink -> ContentType.RichLink
        is Message.QuickReply -> ContentType.QuickReply
        is Message.Unsupported -> ContentType.Unsupported
    }

internal enum class ContentType {
    Unsupported,
    DateHeader,
    Attachment,
    Text,
    QuickReply,
    Loading,
    ListPicker,
    RichLink,
}
