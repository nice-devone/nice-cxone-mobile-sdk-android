package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal data class AgentModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("inContactId")
    val inContactId: UUID?,

    @SerializedName("emailAddress")
    val emailAddress: String?,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("surname")
    val surname: String,

    @SerializedName("nickname")
    val nickname: String?,

    @SerializedName("isBotUser")
    val isBotUser: Boolean,

    @SerializedName("isSurveyUser")
    val isSurveyUser: Boolean,

    @SerializedName("imageUrl")
    val imageUrl: String,
) {

    fun toAgent(): Agent = AgentInternal(
        id = id,
        inContactId = inContactId,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = surname,
        nickname = nickname,
        isBotUser = isBotUser,
        isSurveyUser = isSurveyUser,
        imageUrl = imageUrl,
        isTyping = false
    )

    fun toMessageAuthor(): MessageAuthor = MessageAuthorInternal(
        id = id.toString(),
        firstName = firstName,
        lastName = surname
    )

}
