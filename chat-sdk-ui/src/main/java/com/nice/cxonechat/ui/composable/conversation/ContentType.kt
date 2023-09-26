/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.ui.composable.conversation.ContentType.ATTACHMENT
import com.nice.cxonechat.ui.composable.conversation.ContentType.LIST_PICKER
import com.nice.cxonechat.ui.composable.conversation.ContentType.PLUGIN
import com.nice.cxonechat.ui.composable.conversation.ContentType.QUICK_REPLY
import com.nice.cxonechat.ui.composable.conversation.ContentType.RICH_LINK
import com.nice.cxonechat.ui.composable.conversation.ContentType.TEXT
import com.nice.cxonechat.ui.composable.conversation.ContentType.UNSUPPORTED
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.Attachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.Plugin
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported

@Stable
internal val Message.contentType: ContentType
    get() = when (this) {
        is Attachment -> ATTACHMENT
        is Text -> TEXT
        is ListPicker -> LIST_PICKER
        is RichLink -> RICH_LINK
        is QuickReply -> QUICK_REPLY
        is Plugin -> PLUGIN
        is Unsupported -> UNSUPPORTED
    }

internal enum class ContentType {
    UNSUPPORTED,
    DATE_HEADER,
    ATTACHMENT,
    TEXT,
    QUICK_REPLY,
    LOADING,
    LIST_PICKER,
    RICH_LINK,
    PLUGIN,
}
