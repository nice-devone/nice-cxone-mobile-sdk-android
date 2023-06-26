package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.AgentInternal
import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal class AgentCopyable(
    private val agent: Agent,
) {

    @Suppress("LongParameterList")
    fun copy(
        id: Int = agent.id,
        inContactId: UUID? = agent.inContactId,
        emailAddress: String? = agent.emailAddress,
        firstName: String = agent.firstName,
        lastName: String = agent.lastName,
        nickname: String? = agent.nickname,
        isBotUser: Boolean = agent.isBotUser,
        isSurveyUser: Boolean = agent.isSurveyUser,
        imageUrl: String = agent.imageUrl,
        isTyping: Boolean = agent.isTyping,
    ) = AgentInternal(
        id = id,
        inContactId = inContactId,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = lastName,
        nickname = nickname,
        isBotUser = isBotUser,
        isSurveyUser = isSurveyUser,
        imageUrl = imageUrl,
        isTyping = isTyping
    )

    companion object {

        fun Agent.asCopyable() =
            AgentCopyable(this)
    }
}
