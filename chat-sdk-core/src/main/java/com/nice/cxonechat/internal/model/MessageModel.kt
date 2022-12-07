package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageDirectionModel.FromApp
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToApp
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Noop
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text
import com.nice.cxonechat.internal.model.network.UserStatistics
import java.util.Date
import java.util.UUID

internal data class MessageModel(
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,

    @SerializedName("threadIdOnExternalPlatform")
    val threadIdOnExternalPlatform: UUID,

    @SerializedName("messageContent")
    val messageContent: MessagePolyContent,

    @SerializedName("createdAt")
    val createdAt: Date,

    @SerializedName("attachments")
    val attachments: List<AttachmentModel>,

    @SerializedName("direction")
    val direction: MessageDirectionModel,

    @SerializedName("userStatistics")
    val userStatistics: UserStatistics,

    @SerializedName("authorUser")
    val authorUser: AgentModel? = null,

    @SerializedName("authorEndUserIdentity")
    val authorEndUserIdentity: CustomerIdentityModel? = null,
) {

    fun toMessage() = when (messageContent) {
        is Plugin -> MessagePlugin(this)
        is Text -> MessageText(this)
        Noop -> null
    }

    companion object {

        val MessageModel.author
            get() = when (direction) {
                FromApp -> authorUser?.toMessageAuthor() ?: MessageAuthorDefaults.User
                ToApp -> authorEndUserIdentity?.toMessageAuthor() ?: MessageAuthorDefaults.Agent
            }

    }

}
