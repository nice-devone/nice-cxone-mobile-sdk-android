package com.nice.cxonechat.internal.model

import com.nice.cxonechat.thread.Agent
import java.util.UUID

internal data class AgentInternal(
    override val id: Int,
    override val inContactId: UUID?,
    override val emailAddress: String?,
    override val firstName: String,
    override val lastName: String,
    override val nickname: String?,
    override val isBotUser: Boolean,
    override val isSurveyUser: Boolean,
    override val imageUrl: String,
    override val isTyping: Boolean,
) : Agent() {

    override fun toString() = buildString {
        append("Agent(id=")
        append(id)
        append(", inContactId=")
        append(inContactId)
        append(", emailAddress=")
        append(emailAddress)
        append(", firstName='")
        append(firstName)
        append("', lastName='")
        append(lastName)
        append("', nickname=")
        append(nickname)
        append(", isBotUser=")
        append(isBotUser)
        append(", isSurveyUser=")
        append(isSurveyUser)
        append(", imageUrl='")
        append(imageUrl)
        append("', isTyping=")
        append(isTyping)
        append(")")
    }
}
