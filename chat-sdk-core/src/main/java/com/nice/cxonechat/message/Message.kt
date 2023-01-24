package com.nice.cxonechat.message

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.Public
import java.util.Date
import java.util.UUID

/**
 * Represents all information about a message in chat. Messages can be either
 * systemic or user generated. There shouldn't be a distinction on client
 * side about those messages, though you should know they are created in
 * different ways.
 *
 * As long as you receive this object from whatever source, you have a
 * guarantee that this object exists somewhere on the server. Messages
 * are never generated locally as phantoms.
 *
 * @see Message.Text
 * @see Message.Plugin
 */
@Public
sealed class Message {
    /**
     * Id that was assigned to this message. If other message matches this
     * id it's the same message with possibly different content.
     * */
    abstract val id: UUID

    /**
     * The thread id that this message belongs to. Messages should never be
     * mismatched between threads as this can lead to inconsistencies and
     * undefined behavior.
     * */
    abstract val threadId: UUID

    /**
     * The timestamp of when the message was created on the server. Similarly
     * to [id] this field shouldn't change for the lifetime of the message.
     * */
    abstract val createdAt: Date

    /**
     * The direction in which the message is sent.
     *
     * @see MessageDirection
     * */
    abstract val direction: MessageDirection

    /**
     * The otherwise uncategorizable properties for this message. It can
     * contain anything from message status to custom properties.
     *
     * @see MessageMetadata
     * */
    abstract val metadata: MessageMetadata

    /**
     * Author associated with this message.
     *
     * @see MessageAuthor
     * */
    abstract val author: MessageAuthor

    /**
     * Attachments provided with the message. This field can be empty.
     * It contains attachments that the user or agent sent alongside with
     * the message.
     *
     * Never make any assumption on the implemented type of the [Iterable].
     *
     * @see ChatThreadMessageHandler.send
     * */
    abstract val attachments: Iterable<Attachment>

    // ---

    /**
     * Simple message representing only text content. This type of content
     * is usually generated through User or Agent replying to each other.
     * */
    @Public
    abstract class Text : Message() {

        /**
         * Text string provided while creating a message. Note it may contain
         * characters out of scope for your current device (typically emojis
         * or unsupported characters from some languages). Use support
         * libraries to display them, if applicable.
         * */
        abstract val text: String
    }

    /**
     * Rich content messages, called plugins. These are very often generated
     * by Agent action in the agent console. Your application can support all
     * [PluginElement] or just focus on the components that is currently
     * required by your own specification. Note that new elements can be added
     * to the backend services for which you need to update the SDK. New
     * elements, previously undefined are ignored by the SDK until implemented.
     * */
    @Public
    abstract class Plugin : Message() {

        /**
         * Additional information provided alongside with the message. Refer to
         * information given by a representative to correctly use this parameter.
         * */
        abstract val postback: String?

        /**
         * Element provided with this message. If the element **is null**,
         * then you have received a message with an element that's not supported by
         * this version. Kindly update the SDK in order to gain support.
         *
         * @see PluginElement
         * */
        abstract val element: PluginElement?
    }
}
