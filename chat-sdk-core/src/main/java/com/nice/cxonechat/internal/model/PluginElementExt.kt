package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement

internal fun PluginElement(element: MessagePolyElement) = when (element) {
    is MessagePolyElement.Button -> PluginElementButton(element)
    is MessagePolyElement.Countdown -> PluginElementCountdown(element)
    is MessagePolyElement.Custom -> PluginElementCustom(element)
    is MessagePolyElement.File -> PluginElementFile(element)
    is MessagePolyElement.InactivityPopup -> PluginElementInactivityPopup(element)
    is MessagePolyElement.Menu -> PluginElementMenu(element)
    is MessagePolyElement.QuickReplies -> PluginElementQuickReplies(element)
    is MessagePolyElement.Subtitle -> PluginElementSubtitle(element)
    is MessagePolyElement.Text -> PluginElementText(element)
    is MessagePolyElement.TextAndButtons -> PluginElementTextAndButtons(element)
    is MessagePolyElement.Title -> PluginElementTitle(element)
    MessagePolyElement.Noop -> null
}
