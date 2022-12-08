package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.thread.ChatThread

internal data class EventThreadRecovered(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val agent get() = postback.data.ownerAssignee?.toAgent()
    val messages get() = postback.data.messages.mapNotNull(MessageModel::toMessage)
    val thread get() = postback.data.thread.toChatThread()
    val scrollToken get() = postback.data.messagesScrollToken

    fun inThread(thread: ChatThread) = this.thread.id == thread.id &&
            messages.all { it.threadId == thread.id }

    data class Data(
        @SerializedName("messages")
        val messages: List<MessageModel>,
        @SerializedName("ownerAssignee")
        val ownerAssignee: AgentModel?,
        @SerializedName("thread")
        val thread: ReceivedThreadData,
        @SerializedName("messagesScrollToken")
        val messagesScrollToken: String,
    )

}
