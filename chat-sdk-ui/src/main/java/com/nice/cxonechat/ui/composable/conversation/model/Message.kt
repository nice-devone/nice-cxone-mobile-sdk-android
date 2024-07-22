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

package com.nice.cxonechat.ui.composable.conversation.model

import android.content.Context
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.message.Message.QuickReplies
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.util.toShortDateString
import java.util.Date
import com.nice.cxonechat.message.Attachment as SdkAttachment
import com.nice.cxonechat.message.Message as SdkMessage

/**
 * UI representation of [SdkMessage].
 *
 * @param original Source [SdkMessage].
 */
internal sealed class Message(original: SdkMessage) {
    /** See [SdkMessage.id]. */
    val id = original.id

    /** Friendly name of sender.  See [SdkMessage.author] */
    val sender: String? = original.author?.name

    /** See [com.nice.cxonechat.message.MessageAuthor.imageUrl]. */
    private val imageUrl: String? = original.author?.imageUrl

    /** See [SdkMessage.createdAt]. */
    val createdAt: Date = original.createdAt

    /** The status of message. */
    val status: MessageStatus = original.metadata.status

    /** See [SdkMessage.direction]. */
    val direction: MessageDirection = original.direction

    /** See [SdkMessage.fallbackText]. */
    val fallbackText: String? = original.fallbackText

    /**
     * [createdAt] formatted as a date string.
     *
     * @param context Context used to resolve the short date format according to the current user settings.
     */
    fun createdAtDate(context: Context): String = context.toShortDateString(createdAt)

    /**
     * UI version of a [SdkMessage.Text] with one or more [SdkAttachment].
     */
    data class WithAttachments(
        private val message: SdkMessage.Text,
    ) : Message(message) {
        /** See [SdkMessage.Text.text]. */
        val text: String = message.text

        /** Attachments to be presented to the user. */
        val attachments: Iterable<SdkAttachment>
            get() = message.attachments
    }

    /**
     * UI version of simple [SdkMessage.Text].
     */
    @Suppress(
        "MemberNameEqualsClassName" // domain naming
    )
    data class Text(private val message: SdkMessage.Text) : Message(message) {
        /**  See [SdkMessage.Text.text]. */
        val text: String = message.text
    }

    /**
     * A list picker displays a list of items, and information about the items,
     * such as product name, description, and image, in the Messages app on the
     * customer's device.
     * The customer can interact multiple times with one or more items from the list.
     * Each interaction should send a reply (on behalf of the user) together with the postback value.
     */
    data class ListPicker(
        private val message: SdkMessage.ListPicker,
        private val sendMessage: (OutboundMessage) -> Unit,
    ) : Message(message) {
        /** Title of the List Picker in the conversation. */
        val title: String = message.title

        /** Additional text to be displayed after clicking on the picker list. */
        val text: String = message.text

        /** List of options to be displayed to the user. */
        val actions: List<Action> = message.actions.mapNotNull { action ->
            action.toUiAction(sendMessage)
        }
    }

    /**
     * A RichLink message to display.
     *
     * Each RichLink message has a title and media image to display in conjunction
     * with an associated URL.  If the message is touched, then the URL should be
     * opened.
     */
    data class RichLink(private val message: SdkMessage.RichLink) : Message(message) {
        /** image media information to display in RichLink. */
        val media: Media = message.media

        /** title to display. */
        val title: String = message.title

        /** URL to open if the item is selected. */
        val url: String = message.url
    }

    /**
     * UI Version of [SdkMessage.QuickReplies].
     */
    data class QuickReply(
        private val message: QuickReplies,
        private val sendMessage: (OutboundMessage) -> Unit
    ) : Message(message) {
        /** title to be displayed. */
        val title = message.title

        /** iterable of actions to be displayed. */
        val actions: List<Action> = message.actions.mapNotNull { action ->
            action.toUiAction(sendMessage)
        }
    }

    /**
     * Default class used for messages which are not yet supported in the UI.
     */
    class Unsupported(message: SdkMessage) : Message(message)
}
