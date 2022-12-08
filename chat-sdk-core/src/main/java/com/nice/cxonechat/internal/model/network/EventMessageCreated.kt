package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.Contact
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.thread.ChatThread

/** Event Received when a message has been successfully sent/created. */
internal data class EventMessageCreated(
    @SerializedName("data")
    val data: Data,
) {

    val contactId get() = data.case.id
    val contactStatus get() = data.case.status

    val threadId get() = data.thread.idOnExternalPlatform
    val threadName get() = data.thread.threadName

    val message get() = data.message.toMessage()

    fun inThread(thread: ChatThread): Boolean {
        return thread.id == threadId
    }

    data class Data(
        @SerializedName("case")
        val case: Contact,
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("message")
        val message: MessageModel,
    )

}
