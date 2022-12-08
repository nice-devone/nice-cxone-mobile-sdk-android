package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.MessageModel.Companion.author
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.PluginElement
import java.util.Date
import java.util.UUID

internal data class MessagePlugin(
    private val model: MessageModel,
) : Plugin() {

    private val content
        get() = model.messageContent as MessagePolyContent.Plugin

    override val id: UUID
        get() = model.idOnExternalPlatform
    override val threadId: UUID
        get() = model.threadIdOnExternalPlatform
    override val createdAt: Date
        get() = model.createdAt
    override val direction: MessageDirection
        get() = model.direction.toMessageDirection()
    override val metadata: MessageMetadata
        get() = model.userStatistics.toMessageMetadata()
    override val author: MessageAuthor
        get() = model.author
    override val attachments: Iterable<Attachment>
        get() = model.attachments.map(AttachmentModel::toAttachment)
    override val postback: String?
        get() = content.payload.postback
    override val elements: Iterable<PluginElement>
        get() = content.payload.elements
            .mapNotNull(::PluginElement)

    override fun toString() = buildString {
        append("Message.Plugin(id=")
        append(id)
        append(", threadId=")
        append(threadId)
        append(", createdAt=")
        append(createdAt)
        append(", direction=")
        append(direction)
        append(", metadata=")
        append(metadata)
        append(", author=")
        append(author)
        append(", attachments=")
        append(attachments)
        append(", postback=")
        append(postback)
        append(", elements=")
        append(elements)
        append(")")
    }

}
