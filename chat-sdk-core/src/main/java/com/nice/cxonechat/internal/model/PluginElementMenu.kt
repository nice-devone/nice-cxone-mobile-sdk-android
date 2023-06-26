package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Menu

internal data class PluginElementMenu(
    private val element: MessagePolyElement.Menu,
) : Menu() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::PluginElement)

    override val files: Iterable<File>
        get() = pluginElements
            .filterIsInstance<File>()
            .asIterable()
    override val titles: Iterable<Title>
        get() = pluginElements
            .filterIsInstance<Title>()
            .asIterable()
    override val subtitles: Iterable<Subtitle>
        get() = pluginElements
            .filterIsInstance<Subtitle>()
            .asIterable()
    override val texts: Iterable<Text>
        get() = pluginElements
            .filterIsInstance<Text>()
            .asIterable()
    override val buttons: Iterable<Button>
        get() = pluginElements
            .filterIsInstance<Button>()
            .asIterable()

    override fun toString() = buildString {
        append("PluginElement.Menu(files=")
        append(files.toList())
        append(", titles=")
        append(titles.toList())
        append(", subtitles=")
        append(subtitles.toList())
        append(", texts=")
        append(texts.toList())
        append(", buttons=")
        append(buttons.toList())
        append(")")
    }
}
