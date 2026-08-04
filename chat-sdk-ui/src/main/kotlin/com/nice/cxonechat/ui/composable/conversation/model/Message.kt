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

package com.nice.cxonechat.ui.composable.conversation.model

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.message.Message.QuickReplies
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.domain.model.asPerson
import com.nice.cxonechat.ui.util.preview.message.SdkAttachment
import com.nice.cxonechat.ui.util.preview.message.SdkListPicker
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.SdkMessageUnsupported
import com.nice.cxonechat.ui.util.preview.message.SdkReplyButton
import com.nice.cxonechat.ui.util.preview.message.SdkRichLink
import com.nice.cxonechat.ui.util.preview.message.SdkText
import com.nice.cxonechat.ui.util.preview.message.SdkTimePicker
import com.nice.cxonechat.ui.util.toShortDateTimeString
import kotlin.time.Instant

/**
 * UI representation of [SdkMessage].
 *
 * @param original Source [SdkMessage].
 */
internal sealed class Message(original: SdkMessage) {
    /** See [com.nice.cxonechat.message.Message.id]. */
    val id = original.id

    /** Details of the sender. */
    val sender: Person? = original.author?.asPerson

    /** See [com.nice.cxonechat.message.MessageAuthor.imageUrl]. */
    private val imageUrl: String? = original.author?.imageUrl

    /** See [com.nice.cxonechat.message.Message.createdAt]. */
    val createdAt: Instant = original.createdAt

    /** The status of message. */
    val status: MessageStatus = original.metadata.status

    /** See [com.nice.cxonechat.message.Message.direction]. */
    val direction: MessageDirection = original.direction

    /** See [com.nice.cxonechat.message.Message.fallbackText]. */
    val fallbackText: String? = original.fallbackText

    /** See [com.nice.cxonechat.message.Message.isAiGenerated]. */
    val isAiGenerated: Boolean = original.isAiGenerated

    /** Accessibility text to be read by screen readers, based on content without usage of string resources. */
    abstract val accessibilityText: String?

    /**
     * [createdAt] formatted as a short date-and-time string (e.g. "Today, 10:30 AM").
     *
     * Used both as the section grouping key in conversation lists and as the timestamp
     * in accessibility announcements (see [getMessageAccessibility]).
     *
     * @param resources Resources used to resolve the locale for the short date-time format according to the current user settings.
     */
    fun createdAtDate(resources: Resources): String = resources.toShortDateTimeString(createdAt)

    internal interface AttachmentsMessage {
        val attachments: Iterable<SdkAttachment>
    }

    /**
     * UI version of a [SdkText] with one or more [SdkAttachment].
     */
    data class WithAttachments(
        private val message: SdkText,
        override val attachments: Iterable<SdkAttachment>,
    ) : Message(message), AttachmentsMessage {
        override val accessibilityText: String? = null // Unused since the attachments themselves will have their own accessibility text.
    }

    data class AudioAttachment(
        private val message: SdkText,
        val attachment: SdkAttachment,
    ) : Message(message), AttachmentsMessage {
        override val attachments: Iterable<SdkAttachment> = listOf(attachment)
        override val accessibilityText: String? = null
    }

    /**
     * UI version of simple [SdkText].
     */
    @Suppress(
        "MemberNameEqualsClassName" // domain naming
    )
    data class Text(private val message: SdkText) : Message(message) {
        /**  See [com.nice.cxonechat.message.Message.Text.text]. */
        val text: String = message.text
        override val accessibilityText: String = text
    }

    /**
     * Special version [Text] message which only contains up to 3 emoji UTF-8 characters.
     */
    data class EmojiText(private val message: SdkText) : Message(message) {
        /**  See [com.nice.cxonechat.message.Message.Text.text]. */
        val text: String = message.text
        override val accessibilityText = text
    }

    /**
     * A list picker displays a list of items, and information about the items,
     * such as product name, description, and image, in the Messages app on the
     * customer's device.
     * The customer can interact multiple times with one or more items from the list.
     * Each interaction should send a reply (on behalf of the user) together with the postback value.
     */
    data class ListPicker(
        private val message: SdkListPicker,
        private val sendMessage: (SdkReplyButton) -> Unit,
    ) : Message(message) {
        /** Title of the List Picker in the conversation. */
        val title: String = message.title

        /** Additional text to be displayed after clicking on the picker list. */
        val text: String = message.text

        /** List of options to be displayed to the user. */
        val actions: List<Action> = message.actions.mapNotNull { action ->
            action.toUiAction(sendMessage)
        }
        override val accessibilityText = "$title.\n$text."
    }

    data class TimePicker(
        private val message: SdkTimePicker,
    ) : Message(message) {
        /** title message to display. */
        val title: String = message.title

        /** title to display on Time Picker popup. */
        val popupTitle: String = message.popupTitle

        /** List of time slots to be displayed to the user. */
        val timeSlots: Iterable<TimeSlot> = message.timeSlots
        override val accessibilityText = title
    }

    /**
     * A RichLink message to display.
     *
     * Each RichLink message has a title and media image to display in conjunction
     * with an associated URL.  If the message is touched, then the URL should be
     * opened.
     */
    data class RichLink(private val message: SdkRichLink) : Message(message) {
        /** image media information to display in RichLink. */
        val media: Media = message.media

        /** title to display. */
        val title: String = message.title

        /** URL to open if the item is selected. */
        val url: String = message.url

        override val accessibilityText = title
    }

    /**
     * UI Version of [com.nice.cxonechat.ui.util.preview.message.SdkQuickReply].
     */
    data class QuickReply(
        private val message: QuickReplies,
        private val sendMessage: (SdkReplyButton) -> Unit,
    ) : Message(message) {
        /** title to be displayed. */
        val title = message.title

        /** iterable of actions to be displayed. */
        val actions: List<Action> = message.actions.mapNotNull { action ->
            action.toUiAction(sendMessage)
        }
        override val accessibilityText = title
    }

    /**
     * Default class used for messages which are not yet supported in the UI.
     */
    class Unsupported(message: SdkMessage) : Message(message) {
        val text: String? = (message as? SdkMessageUnsupported)?.text ?: fallbackText
        override val accessibilityText: String? = text
    }
}

/**
 * Generates an accessibility description for a message, including its text, timestamp.
 *
 * @receiver The message for which to generate the description.
 * @param overrideText Optional text to use instead of the message's own text for accessibility purposes.
 * This can be used for messages that have complex content (e.g., attachments) where a localized description is more appropriate.
 * Default value is null, which is ignored and the [Message.accessibilityText] (or empty string) is used instead.
 * @return A string describing the message for accessibility purposes.
 */
@Composable
internal fun Message.getMessageAccessibility(overrideText: String? = null): String {
    val resources = LocalResources.current
    val toAgent = direction === MessageDirection.ToAgent
    val timeString = createdAtDate(resources)

    val messageText = overrideText ?: accessibilityText.orEmpty()

    return if (toAgent) {
        stringResource(
            id = R.string.accessibility_message_you,
            messageText,
            timeString
        )
    } else {
        val agentName = when {
            isAiGenerated -> stringResource(id = R.string.ai_assistant_label)
            else -> sender?.name?.ifBlank { null } ?: stringResource(id = R.string.accessibility_no_agent_name)
        }
        stringResource(
            id = R.string.accessibility_message_from_agent,
            agentName,
            messageText,
            timeString
        )
    }
}
