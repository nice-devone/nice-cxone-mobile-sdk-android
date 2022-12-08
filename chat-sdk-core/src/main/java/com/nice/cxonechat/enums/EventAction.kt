package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * The different types of actions for an event.
 */
internal enum class EventAction(val value: String) {

    /** The customer is registering for chat access. */
    @SerializedName("register")
    Register("register"),

    /** The customer is interacting with something in the chat window. */
    @SerializedName("chatWindowEvent")
    ChatWindowEvent("chatWindowEvent"),

    /** The customer is making an outbound action. */
    @SerializedName("outbound")
    Outbound("outbound"),

    /** The socket is sending a message to verify the connection. */
    @SerializedName("heartbeat")
    Heartbeat("heartbeat"),
}
