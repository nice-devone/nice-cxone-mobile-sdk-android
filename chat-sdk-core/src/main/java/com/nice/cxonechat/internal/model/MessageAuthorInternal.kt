package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.MessageAuthor

internal data class MessageAuthorInternal(
    override val id: String,
    override val firstName: String,
    override val lastName: String,
) : MessageAuthor() {

    override fun toString() = buildString {
        append("MessageAuthor(id='")
        append(id)
        append("', firstName='")
        append(firstName)
        append("', lastName='")
        append(lastName)
        append("')")
    }

}
