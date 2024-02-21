/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.conversation.plugin

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.SENDING
import com.nice.cxonechat.message.PluginElement
import com.nice.cxonechat.message.PluginElement.QuickReplies
import com.nice.cxonechat.message.TextFormat
import com.nice.cxonechat.message.TextFormat.Html
import com.nice.cxonechat.message.TextFormat.Markdown
import com.nice.cxonechat.message.TextFormat.Plain
import com.nice.cxonechat.ui.composable.conversation.model.Message
import java.util.Date
import java.util.UUID

internal open class MessagePreviewProvider: PreviewParameterProvider<Message> {
    data class File(
        override val url: String,
        override val name: String,
        override val mimeType: String
    ) : PluginElement.File()

    data class Title(override val text: String) : PluginElement.Title()

    data class Subtitle(override val text: String) : PluginElement.Subtitle()

    data class Text(
        override val text: String,
        override val format: TextFormat = Plain,
    ) : PluginElement.Text() {
        @Deprecated("isMarkdown has been deprecated, please replace with format.", replaceWith = ReplaceWith("format.isMarkdown"))
        override val isMarkdown: Boolean get() = format.isMarkdown

        @Deprecated("isHtml has been deprecated, please replace with format.", replaceWith = ReplaceWith("format.isHtml"))
        override val isHtml: Boolean get() = format.isHtml
    }

    data class Button(
        override val text: String,
        override val postback: String?,
        override val deepLink: String? = null,
        override val displayInApp: Boolean = false
    ) : PluginElement.Button()

    data class Custom(
        override val fallbackText: String?,
        override val variables: Map<String, Any?>
    ) : PluginElement.Custom()

    data class Menu(
        override val files: Iterable<File> = listOf(),
        override val titles: Iterable<Title> = listOf(),
        override val subtitles: Iterable<Subtitle> = listOf(),
        override val texts: Iterable<Text> = listOf(),
        override val buttons: Iterable<Button> = listOf()
    ): PluginElement.Menu()

    data class Gallery(
        override val elements: Iterable<PluginElement>
    ) : PluginElement.Gallery()

    data class TextAndButtons(
        override val text: Text,
        override val buttons: Iterable<Button>
    ) : PluginElement.TextAndButtons()

    data class QuickReply(
        override val text: Text?,
        override val buttons: Iterable<Button>
    ) : QuickReplies()

    data class SatisfactionSurvey(
        override val text: Text?,
        override val button: Button,
        override val postback: String?
    ) : PluginElement.SatisfactionSurvey()

    val menu = Menu(
        files = listOf(
            File(
                url = "https://picsum.photos/300/150",
                name = "photo.jpg",
                mimeType = "image/jpeg"
            )
        ),
        titles = listOf(
            Title(
                "Menu"
            )
        ),
        texts = listOf(
            Text(
                text = "Lorem Ipsum"
            )
        ),
        buttons = listOf(
            Button(
                text = "Click me!",
                postback = "click-on-button-1"
            ),
            Button(
                text = "No click me!",
                postback = "click-on-button-2"
            ),
            Button(
                text = "Aww don`t click on me",
                postback = "click-on-button-3"
            )
        )
    )

    val title = Title("Some Title")

    val subtitle = Subtitle("Some Subtitle")

    val text: List<Message> = listOf(
        Text("Some Text"),
        Text("Some **bold** text.", format = Markdown),
        Text("Some <b>bold</b> text.", format = Html)
    ).map { it.asMessage("text") }

    val button = Button(
        "Button Text",
        "postback-button",
    )

    val custom = Custom(
        fallbackText = "Custom",
        variables = mapOf("color" to "green")
    )

    val file = File(
        url = "https://picsum.photos/300/150",
        name = "photo.jpg",
        mimeType = "image/jpeg"
    )

    val textAndButtons = TextAndButtons(
        text = Text("Text And Buttons"),
        buttons = listOf(
            Button("Button 1", "button-1"),
            Button("Button 2", "button-2")
        )
    )

    val quickReply = QuickReply(
        text = Text("Quick Reply"),
        buttons = listOf(
            Button("Button 1", "button-1"),
            Button("Button 2", "button-2")
        )
    )

    val satisfactionSurvey = SatisfactionSurvey(
        text = Text(text = "Lorem Ipsum"),
        button = Button(text = "Click Me", postback = "click-on-survey"),
        postback = "Post back",
    )

    val gallery = Gallery(listOf(menu, menu, menu))

    private val elements: List<Pair<String, PluginElement>> = listOf(
        "Button" to button,
        "Custom" to custom,
        "File" to file,
        "TextAndButtons" to textAndButtons,
        "QuickReply" to quickReply,
        "Menu" to menu,
        "Gallery" to gallery,
        "SatisfactionSurvey" to satisfactionSurvey
    )

    override val values: Sequence<Message>
        get() = elements.map { (name, element) ->
            element.asMessage(name)
        }.asSequence()
}

internal fun PluginElement.asMessage(name: String) = Message.Plugin(
    object : Plugin() {
        override val id = UUID.randomUUID()
        override val threadId = UUID.randomUUID()
        override val createdAt = Date()
        override val direction = MessageDirection.values().random()
        override val metadata = object : MessageMetadata {
            override val seenAt: Date? = null
            override val readAt = null
            override val status: MessageStatus = SENDING
        }
        override val author: MessageAuthor? = object : MessageAuthor() {
            override val id = ""
            override val firstName = "firstname"
            override val lastName = "lastname"
            override val imageUrl = null
        }
        override val attachments = emptyList<Attachment>()
        override val fallbackText = "Fallback"
        override val postback = name
        override val element = this@asMessage
    }
) {}
