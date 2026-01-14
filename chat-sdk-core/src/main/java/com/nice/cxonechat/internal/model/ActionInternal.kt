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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.ButtonElement
import com.nice.cxonechat.internal.model.network.PolyAction
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Media

internal sealed class ActionInternal : Action {

    /**
     * Action which should be sent as a message with a text content type.
     */
    internal data class ReplyButton(
        override val text: String,
        override val postback: String?,
        override val media: Media?,
        override val description: String?,
    ) : ActionInternal(), Action.ReplyButton {
        constructor(model: PolyAction.ReplyButton) : this(
            text = model.text,
            postback = model.postback,
            media = model.media?.let(::MediaInternal),
            description = model.description
        )
    }

    /**
     * Action which should be sent as a message with postback content type.
     */
    internal data class PostbackReplyButton(
        override val text: String,
        override val postback: String?,
    ) : ActionInternal(), Action.ReplyButton {
        override val media: Media? = null
        override val description: String? = null

        constructor(model: ButtonElement) : this(
            text = model.text,
            postback = model.postback
        )
    }

    companion object {
        fun create(model: PolyAction): ActionInternal = when (model) {
            is PolyAction.ReplyButton -> ReplyButton(model)
        }

        fun create(element: ButtonElement): Action.ReplyButton = PostbackReplyButton(element)
    }
}
