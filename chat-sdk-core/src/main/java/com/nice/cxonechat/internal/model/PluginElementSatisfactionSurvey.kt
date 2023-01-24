package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement

internal data class PluginElementSatisfactionSurvey(
    private val element: MessagePolyElement.SatisfactionSurvey,
) : PluginElement.SatisfactionSurvey() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::PluginElement)

    override val text: Text?
        get() = pluginElements
            .filterIsInstance<Text>()
            .firstOrNull()
    override val button: Button
        get() = pluginElements
            .filterIsInstance<Button>()
            .first()
    override val postback: String?
        get() = element.postback

    override fun toString() = buildString {
        append("PluginElement.SatisfactionSurvey(text=")
        append(text)
        append(", button=")
        append(button)
        append(", postback=")
        append(postback)
        append("')")
    }

    internal companion object {
        @Suppress("KotlinConstantConditions")
        fun createVerifiedInstance(element: MessagePolyElement.SatisfactionSurvey) =
            if (element.elements.any { it is MessagePolyElement.Button }) {
                PluginElementSatisfactionSurvey(element)
            } else {
                null
            }
    }
}
