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

package com.nice.cxonechat.ui.composable.conversation.model

import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Html
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Markdown
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Plain
import okhttp3.internal.toImmutableMap
import java.util.Date
import javax.annotation.concurrent.Immutable
import com.nice.cxonechat.message.PluginElement as SdkPluginElement

@Immutable
internal sealed class PluginElement {
    @Immutable
    data class Menu(
        val files: Iterable<File>,
        val titles: Iterable<Title>,
        val subtitles: Iterable<Subtitle>,
        val texts: Iterable<Text>,
        val buttons: Iterable<Button>,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Menu, sendMessage: (OutboundMessage) -> Unit) : this(
            files = element.files.map(::File),
            titles = element.titles.map(::Title),
            subtitles = element.subtitles.map(::Subtitle),
            texts = element.texts.map(::Text),
            buttons = element.buttons.map { Button(it, sendMessage) },
        )
    }

    @Immutable
    data class File(
        val url: String,
        val name: String,
        val mimeType: String,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.File) : this(
            url = element.url,
            name = element.name,
            mimeType = element.mimeType
        )
    }

    @Immutable
    data class Title(
        val text: String
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Title) : this(
            text = element.text
        )
    }

    @Immutable
    data class Subtitle(
        val text: String
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Subtitle) : this(
            text = element.text
        )
    }

    @Immutable
    data class Text(
        val text: String,
        val format: Format,
    ) : PluginElement() {
        enum class Format {
            Plain,
            Markdown,
            Html
        }

        constructor(element: SdkPluginElement.Text) : this(
            text = element.text,
            format = when {
                element.isMarkdown -> Markdown
                element.isHtml -> Html
                else -> Plain
            }
        )
    }

    @Immutable
    data class Button(
        val text: String,
        val deepLink: String?,
        val displayInApp: Boolean,
        val action: ButtonAction?
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Button, sendMessage: (OutboundMessage) -> Unit) : this(
            text = element.text,
            deepLink = element.deepLink,
            displayInApp = element.displayInApp,
            action = compoundAction(
                sendMessageAction(sendMessage, element.text, element.postback),
                deepLinkAction(element.deepLink)
            )
        )
    }

    @Immutable
    data class TextAndButtons(
        val text: Text,
        val buttons: Iterable<Button>,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.TextAndButtons, sendMessage: (OutboundMessage) -> Unit) : this(
            text = Text(element.text),
            buttons = element.buttons.map { Button(it, sendMessage) }
        )
    }

    @Immutable
    data class QuickReplies(
        val text: Text?,
        val buttons: Iterable<Button>,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.QuickReplies, sendMessage: (OutboundMessage) -> Unit) : this(
            text = element.text?.let(::Text),
            buttons = element.buttons.map { Button(it, sendMessage) }
        )
    }

    @Immutable
    data class InactivityPopup(
        val title: Title,
        val subtitle: Subtitle?,
        val texts: Iterable<Text>,
        val buttons: Iterable<Button>,
        val countdown: Countdown,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.InactivityPopup, sendMessage: (OutboundMessage) -> Unit) : this(
            title = Title(element.title),
            subtitle = element.subtitle?.let(::Subtitle),
            texts = element.texts.map(::Text),
            buttons = element.buttons.map { Button(it, sendMessage) },
            countdown = Countdown(element.countdown)
        )
    }

    @Immutable
    data class Countdown(
        val endsAt: Date,
        val isExpired: Boolean,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Countdown) : this(
            endsAt = element.endsAt,
            isExpired = element.isExpired
        )
    }

    @Immutable
    data class Custom(
        val fallbackText: String?,
        val variables: Map<String, Any?>,
        val sendMessage: (OutboundMessage) -> Unit,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Custom, sendMessage: (OutboundMessage) -> Unit) : this(
            fallbackText = element.fallbackText,
            variables = element.variables.toImmutableMap(),
            sendMessage = sendMessage,
        )
    }

    @Immutable
    data class Gallery(
        val elements: Iterable<PluginElement>
    ) : PluginElement() {
        constructor(element: SdkPluginElement.Gallery, sendMessage: (OutboundMessage) -> Unit) : this(
            elements = element.elements.map { invoke(it, sendMessage) }
        )
    }

    @Immutable
    data class SatisfactionSurvey(
        val text: Text?,
        val button: Button,
        val postback: String?,
    ) : PluginElement() {
        constructor(element: SdkPluginElement.SatisfactionSurvey, sendMessage: (OutboundMessage) -> Unit) : this(
            text = element.text?.let(::Text),
            button = Button(element.button, sendMessage),
            postback = element.postback
        )
    }

    @Immutable
    companion object {
        operator fun invoke(element: SdkPluginElement, sendMessage: (OutboundMessage) -> Unit): PluginElement {
            return when (element) {
                is SdkPluginElement.Button -> Button(element = element, sendMessage = sendMessage)
                is SdkPluginElement.Countdown -> Countdown(element = element)
                is SdkPluginElement.Custom -> Custom(element = element, sendMessage = sendMessage)
                is SdkPluginElement.File -> File(element = element)
                is SdkPluginElement.Gallery -> Gallery(element = element, sendMessage = sendMessage)
                is SdkPluginElement.InactivityPopup -> InactivityPopup(element = element, sendMessage = sendMessage)
                is SdkPluginElement.Menu -> Menu(element = element, sendMessage = sendMessage)
                is SdkPluginElement.QuickReplies -> QuickReplies(element = element, sendMessage = sendMessage)
                is SdkPluginElement.SatisfactionSurvey -> SatisfactionSurvey(
                    element = element,
                    sendMessage = sendMessage
                )
                is SdkPluginElement.Subtitle -> Subtitle(element = element)
                is SdkPluginElement.Text -> Text(element = element)
                is SdkPluginElement.TextAndButtons -> TextAndButtons(element = element, sendMessage = sendMessage)
                is SdkPluginElement.Title -> Title(element = element)
            }
        }
    }
}
