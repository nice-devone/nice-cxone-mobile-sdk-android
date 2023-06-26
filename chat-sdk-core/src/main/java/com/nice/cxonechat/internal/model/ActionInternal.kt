package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.PolyAction
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Media

internal sealed class ActionInternal : Action {
    internal data class ReplyButton(
        override val text: String,
        override val postback: String?,
        override val media: Media?,
        override val description: String?
    ) : ActionInternal(), Action.ReplyButton {
        constructor(model: PolyAction.ReplyButton) : this(
            text = model.text,
            postback = model.postback,
            media = model.media?.let(::MediaInternal),
            description = model.description
        )
    }

    companion object {
        fun create(model: PolyAction): ActionInternal = when(model) {
            is PolyAction.ReplyButton -> ReplyButton(model)
        }
    }
}
