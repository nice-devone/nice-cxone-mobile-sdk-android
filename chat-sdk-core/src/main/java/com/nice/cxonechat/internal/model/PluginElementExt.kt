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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement

internal fun createPluginElement(element: MessagePolyElement) = when (element) {
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
    is MessagePolyElement.SatisfactionSurvey -> PluginElementSatisfactionSurvey.createVerifiedInstance(element)
    MessagePolyElement.Noop -> null
}
