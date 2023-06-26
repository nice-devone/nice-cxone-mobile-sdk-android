package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.thread.ChatThread

internal data class EventMoreMessagesLoaded(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val scrollToken get() = postback.data.scrollToken
    val messages get() = postback.data.messages.mapNotNull(MessageModel::toMessage)

    fun inThread(thread: ChatThread) = messages.all { it.threadId == thread.id }

    data class Data(
        @SerializedName("messages")
        val messages: List<MessageModel>,
        @SerializedName("scrollToken")
        val scrollToken: String,
    )
}
