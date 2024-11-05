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

package com.nice.cxonechat.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The different types of visitor events.
 */
@Serializable
internal enum class VisitorEventType(val value: String) {
    /** Event for the visitor starting a new page visit. */
    @SerialName("VisitorVisit")
    VisitorVisit("VisitorVisit"),

    /** Event for the visitor viewing a page. */
    @SerialName("PageView")
    PageView("PageView"),

    /** Event for time visitor sent on a page. */
    @SerialName("TimeSpentOnPage")
    TimeSpentOnPage("TimeSpentOnPage"),

    /** Event that the chat window was opened by the visitor. */
    @SerialName("ChatWindowOpened")
    ChatWindowOpened("ChatWindowOpened"),

    /** Event that the visitor has followed a proactive action to start a chat. */
    @SerialName("Conversion")
    Conversion("Conversion"),

    /** Event that the proactive action was successfully displayed to the visitor. */
    @SerialName("ProactiveActionDisplayed")
    ProactiveActionDisplayed("ProactiveActionDisplayed"),

    /** Event that the proactive action was clicked by the visitor. */
    @SerialName("ProactiveActionClicked")
    ProactiveActionClicked("ProactiveActionClicked"),

    /** Event that the proactive action has successfully led to a conversion. */
    @SerialName("ProactiveActionSuccess")
    ProactiveActionSuccess("ProactiveActionSuccess"),

    /** Event that the proactive action has not led to a conversion within a certain time span. */
    @SerialName("ProactiveActionFailed")
    ProactiveActionFailed("ProactiveActionFailed"),

    /** A custom visitor event to send any additional data. */
    Custom("Custom")
}
