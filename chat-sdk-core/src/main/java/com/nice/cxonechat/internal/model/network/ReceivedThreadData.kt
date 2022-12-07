package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.Thread
import java.util.Date
import java.util.UUID

internal data class ReceivedThreadData(
    @SerializedName("id")
    internal val id: String,
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("threadName")
    val threadName: String,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("updatedAt")
    val updatedAt: Date,
    @SerializedName("canAddMoreMessages")
    val canAddMoreMessages: Boolean,
    @SerializedName("thread")
    val thread: Thread,
) {

    fun toChatThread() = ChatThreadInternal(
        id = idOnExternalPlatform,
        messages = mutableListOf(),
        canAddMoreMessages = canAddMoreMessages,
        threadName = threadName
    )
}
