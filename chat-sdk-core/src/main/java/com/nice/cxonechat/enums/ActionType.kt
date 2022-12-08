package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * The different types of WebSocket actions.
 */
internal enum class ActionType(val value: String) {
    /** An action for welcome message. */
    @SerializedName("WelcomeMessage")
    WelcomeMessage("WelcomeMessage"),

    /** An action for custom popup box. */
    @SerializedName("CustomPopupBox")
    CustomPopupBox("CustomPopupBox")
}
