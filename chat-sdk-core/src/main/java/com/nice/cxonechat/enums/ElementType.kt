package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * The different types of elements that can be present in the content of a message.
 */
enum class ElementType(value: String) {

    /** Basic text. */
    @SerializedName("TEXT")
    Text("TEXT"),

    /** A button that the customer can press. */
    @SerializedName("BUTTON")
    Button("BUTTON"),

    /** A file that the customer can view. */
    @SerializedName("FILE")
    File("FILE"),

    /** A title to display. */
    @SerializedName("TITLE")
    Title("TITLE"),

    /** A menu plugin to display. */
    @SerializedName("MENU")
    Menu("MENU"),

    /** A quick reply plugin to display. */
    @SerializedName("QUICK_REPLIES")
    QuickReplies("QUICK_REPLIES"),

    /** A countdown plugin. */
    @SerializedName("COUNTDOWN")
    Countdown("COUNTDOWN"),

    /** A plugin to display when the customer is inactive. */
    @SerializedName("INACTIVITY_POPUP")
    InactivityPopup("INACTIVITY_POPUP"),

    /** A custom plugin that is displayed. */
    @SerializedName("CUSTOM")
    Custom("CUSTOM"),

    /** A satisfaction survey plugin to display. */
    @SerializedName("SATISFACTION_SURVEY")
    SatisfactionSurvey("SATISFACTION_SURVEY")
}
