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

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.Public
import java.util.Date
import java.util.UUID

/**
 * Represents all information about a message in chat. Messages can be either
 * systemic or user generated. There shouldn't be a distinction on the client
 * side about those messages, though you should know they are created in
 * different ways.
 *
 * As long as you receive this object from whatever source, you have a
 * guarantee that this object exists somewhere on the server. Messages
 * are never generated locally as phantoms.
 *
 * @see Message.Text
 * @see Message.RichLink
 * @see Message.ListPicker
 * @see Message.QuickReplies
 */
@Public
sealed class Message {
    /**
     * The id that was assigned to this message.
     * If another message has matching id, it's the same message with possibly different content.
     */
    abstract val id: UUID

    /**
     * The thread id that this message belongs to. Messages should never be
     * mismatched between threads as this can lead to inconsistencies and
     * undefined behavior.
     */
    abstract val threadId: UUID

    /**
     * The timestamp of when the message was created on the server. Similarly
     * to [id] this field shouldn't change for the lifetime of the message.
     */
    abstract val createdAt: Date

    /**
     * The direction in which the message is sent.
     *
     * @see MessageDirection
     */
    abstract val direction: MessageDirection

    /**
     * The otherwise uncategorizable properties for this message. It can
     * contain anything from message status to custom properties.
     *
     * @see MessageMetadata
     */
    abstract val metadata: MessageMetadata

    /**
     * Author associated with this message.
     *
     * *Note:* Under some circumstances the author name may be unknown and [author] will be null.
     * The UI can provide a suitable localized fallback based on [direction].
     *
     * @see MessageAuthor
     */
    abstract val author: MessageAuthor?

    /**
     * Attachments provided with the message. This field can be empty.
     * It contains attachments that the user or agent sent alongside with
     * the message.
     *
     * Never make any assumption on the implemented type of the [Iterable].
     *
     * @see ChatThreadMessageHandler.send
     */
    abstract val attachments: Iterable<Attachment>

    /**
     * Optional fallback text which can be used if UI integration doesn't support concrete subtype of [Message].
     */
    abstract val fallbackText: String?

    // ---

    /**
     * Simple message representing only text content. This type of content
     * is usually generated through User or Agent replying to each other.
     */
    @Public
    abstract class Text : Message() {

        @Suppress(
            "MemberNameEqualsClassName" // Naming required by the domain
        )
        /**
         * Text string provided while creating a message. Note it may contain
         * characters out of scope for your current device (typically emojis
         * or unsupported characters from some languages). Use support
         * libraries to display them, if applicable.
         */
        abstract val text: String
    }

    /**
     * Quick Reply messages have a [title] to present to the user along with
     * a selection of quick response [Action.ReplyButton].
     * The buttons should also be presented to the user, and if the user taps a button, the associated
     * postback should be sent together with the text of the button as a reply message (on behalf of the user).
     * The action can be invoked only once, and the integrating applications have to prevent multiple uses.
     */
    @Public
    abstract class QuickReplies : Message() {
        /** title message to display. */
        abstract val title: String

        /** list of actions to display along with [title]. */
        abstract val actions: Iterable<Action>
    }

    /**
     * A list picker displays a list of items, and information about the items,
     * such as product name, description, and image, in the Messages app on the
     * customer's device.
     * The customer can interact multiple times with one or more items from the list.
     * Each interaction should send a reply (on behalf of the user) together with the postback value.
     */
    @Public
    abstract class ListPicker : Message() {
        /** Title of the List Picker in the conversation. */
        abstract val title: String

        /** Additional text to be displayed after clicking on the picker list. */
        abstract val text: String

        /** List of options to be displayed to the user. */
        abstract val actions: Iterable<Action>
    }

    /**
     * A RichLink message to display.
     *
     * Each RichLink message has a title and media image to display in conjunction
     * with an associated URL.  If the message is touched, then the URL should be
     * opened.
     */
    @Public
    abstract class RichLink : Message() {

        /** image media information to display in RichLink. */
        abstract val media: Media

        /** title to display. */
        abstract val title: String

        /** URL to open if the item is selected. */
        abstract val url: String
    }
}
