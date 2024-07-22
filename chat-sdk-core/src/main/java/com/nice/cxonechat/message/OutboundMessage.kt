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
