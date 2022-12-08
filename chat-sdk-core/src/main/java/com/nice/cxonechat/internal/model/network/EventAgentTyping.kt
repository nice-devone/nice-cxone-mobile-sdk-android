package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.thread.ChatThread

/** Event received when the agent begins typing or stops typing. */
internal data class EventAgentTyping(
    @SerializedName("data")
    val data: Data,
) {

    val agent get() = data.user?.toAgent()

    fun inThread(thread: ChatThread) =
        data.thread.idOnExternalPlatform == thread.id

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("user")
        val user: AgentModel?,
    )

}
