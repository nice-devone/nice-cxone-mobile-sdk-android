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
    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        /**
         * Creates default instance of [OutboundMessage] for simple text messages.
         *
         * @param message see [OutboundMessage.message].
         * @param postback Optional - see [OutboundMessage.postback].
         */
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            message: String,
            postback: String? = null,
        ): OutboundMessage = create(message, postback)

        /**
         * Creates default instance of [OutboundMessage] for attachment messages.
         *
         * @param attachments see [OutboundMessage.attachments], should be non-empty.
         * @param message Optional text message to be sent with attachment.
         * @param postback Optional - see [OutboundMessage.postback].
         */
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            attachments: Iterable<ContentDescriptor>,
            message: String = "",
            postback: String? = null,
        ): OutboundMessage = create(attachments, message, postback)

        /**
         * Creates a default instance of [OutboundMessage] for reply button actions.
         *
         * @param action The reply button action that triggered this message.
         * @return A new instance of [OutboundMessage].
         */
        @JvmStatic
        operator fun invoke(
            action: Action.ReplyButton,
        ): OutboundMessage = create(
            message = action.text,
            postback = action.postback
        )

        /**
         * Creates default instance of [OutboundMessage] for simple text messages.
         *
         * @param message see [OutboundMessage.message].
         * @param postback Optional - see [OutboundMessage.postback].
         */
        @JvmStatic
        @JvmOverloads
        fun create(
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
        @JvmStatic
        @JvmOverloads
        fun create(
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

        internal data class LiveChatBeginOutboundMessage(
            override val message: String,
        ) : OutboundMessage {
            override val attachments: Iterable<ContentDescriptor> = emptyList()
            override val postback: String? = null
            override fun toString(): String = "LiveChatBeginOutboundMessage()"
        }

        internal data class UnsupportedMessageTypeAnswer(
            override val message: String,
            val isUnsupportedMessageTypeAnswer: Boolean,
        ) : OutboundMessage {
            override val attachments: Iterable<ContentDescriptor> = emptyList()
            override val postback: String? = null

            override fun toString(): String =
                "UnsupportedMessageTypeAnswer(isUnsupportedMessageTypeAnswer='$isUnsupportedMessageTypeAnswer')"
        }
    }
}
