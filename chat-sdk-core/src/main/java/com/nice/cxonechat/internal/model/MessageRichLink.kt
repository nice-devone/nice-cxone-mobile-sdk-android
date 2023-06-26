package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.MessageModel.Companion.author
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.message.Message.RichLink
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import java.util.Date
import java.util.UUID

internal data class MessageRichLink(
    private val model: MessageModel,
) : RichLink() {

    private val content
        get() = model.messageContent as MessagePolyContent.RichLink

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

    override val title: String
        get() = content.payload.title.content
    override val url: String
        get() = content.payload.url
    override val fallbackText: String
        get() = content.fallbackText
    override val media: Media
        get() = MediaInternal(content.payload.media)

    override fun toString() = buildString {
        append("Message.RichLink(id=")
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
        append(", title=")
        append(title)
        append(", fallbackText=")
        append(fallbackText)
        append(", url=")
        append(url)
        append(", media=")
        append(media)
        append(")")
    }
}
