package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Message to be sent from the SDK client to server.
 */
@Public
interface OutboundMessage {

    /**
     * Text message to be sent, empty string will be ignored.
     */
    val message: String

    /**
     * Optional attachments to be sent with the message, empty iterable is allowed.
     */
    val attachments: Iterable<ContentDescriptor>

    /**
     * Optional postback, which was part of an [Action] which has triggered this [OutboundMessage].
     */
    val postback: String?

    @Public
    companion object {
        /**
         * Creates default instance of [OutboundMessage] for simple text messages.
         *
         * @param message see [OutboundMessage.message].
         * @param postback Optional - see [OutboundMessage.postback].
         */
        @JvmName("create")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            message: String,
            postback: String? = null,
        ): OutboundMessage = DefaultOutboundMessage(
            message = message,
            postback = postback
        )

        /**
         * Creates default instance of [OutboundMessage] for attachment messages.
         *
         * @param attachments see [OutboundMessage.attachments], should be non-empty.
         * @param message Optional text message to be sent with attachment.
         * @param postback Optional - see [OutboundMessage.postback].
         */
        @JvmName("create")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            attachments: Iterable<ContentDescriptor>,
            message: String = "",
            postback: String? = null,
        ): OutboundMessage = DefaultOutboundMessage(
            message = message,
            attachments = attachments,
            postback = postback
        )

        /**
         * Default implementation of [OutboundMessage].
         * This implementation is private since it is a data class.
         */
        private data class DefaultOutboundMessage(
            override val message: String,
            override val attachments: Iterable<ContentDescriptor> = emptyList(),
            override val postback: String? = null,
        ) : OutboundMessage {
            override fun toString(): String = "OutboundMessage(message='$message', attachments=$attachments, postback=$postback)"
        }
    }
}
