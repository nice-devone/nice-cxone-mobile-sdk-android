package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.thread.ChatThread

internal data class EventThreadMetadataLoaded(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val agent get() = postback.data.ownerAssignee?.toAgent()
    val message get() = postback.data.lastMessage.toMessage()

    fun inThread(thread: ChatThread) = message?.threadId == thread.id

    data class Data(
        @SerializedName("ownerAssignee")
        val ownerAssignee: AgentModel? = null,
        @SerializedName("lastMessage")
        val lastMessage: MessageModel,
    )
}
