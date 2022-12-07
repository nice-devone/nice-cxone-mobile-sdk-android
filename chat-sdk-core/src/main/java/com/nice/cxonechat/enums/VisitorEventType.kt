package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * The different types of visitor events.
 */
internal enum class VisitorEventType(val value: String) {
    /** Event for the visitor starting a new page visit. */
    VisitorVisit("VisitorVisit"),

    /** Event for the visitor viewing a page. */
    @SerializedName("PageView")
    PageView("PageView"),

    /** Event that the chat window was opened by the visitor. */
    ChatWindowOpened("ChatWindowOpened"),

    /** Event that the visitor has followed a proactive action to start a chat. */
    Conversion("Conversion"),

    /** Event that the proactive action was successfully displayed to the visitor. */
    ProactiveActionDisplayed("ProactiveActionDisplayed"),

    /** Event that the proactive action was clicked by the visitor. */
    ProactiveActionClicked("ProactiveActionClicked"),

    /** Event that the proactive action has successfully led to a conversion. */
    ProactiveActionSuccess("ProactiveActionSuccess"),

    /** Event that the proactive action has not led to a conversion within a certain time span. */
    ProactiveActionFailed("ProactiveActionFailed"),

    /** A custom visitor event to send any additional data. */
    Custom("Custom")
}
