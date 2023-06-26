package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.thread.ChatThread

internal data class EventThreadRecovered(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    private val data get() = postback.data
    val agent get() = data.inboxAssignee?.toAgent()
    val messages get() = data.messages.mapNotNull(MessageModel::toMessage)
    val thread get() = data.thread
        .toChatThread()
        .copy(fields = postback.data.contact?.customFields.orEmpty().map(CustomFieldModel::toCustomField))
    val scrollToken get() = data.messagesScrollToken
    val customerCustomFields get() = data.customer?.customFields.orEmpty().map(CustomFieldModel::toCustomField)

    fun inThread(thread: ChatThread) = this.thread.id == thread.id &&
            messages.all { it.threadId == thread.id }

    data class Data(
        @SerializedName("messages")
        val messages: List<MessageModel>,
        @SerializedName("inboxAssignee")
        val inboxAssignee: AgentModel?,
        @SerializedName("thread")
        val thread: ReceivedThreadData,
        @SerializedName("messagesScrollToken")
        val messagesScrollToken: String,
        @SerializedName("customer")
        val customer: CustomFieldsData? = null,
        @SerializedName("consumerContact")
        val contact: CustomFieldsData? = null,
    )
}
