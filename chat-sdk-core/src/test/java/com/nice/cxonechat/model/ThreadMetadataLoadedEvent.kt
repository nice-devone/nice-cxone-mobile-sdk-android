package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.tool.nextString
import java.util.UUID
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt

@Suppress("LongParameterList")
internal fun makeAgent(
    id: Int = nextInt(),
    inContactId: UUID? = UUID.randomUUID(),
    emailAddress: String? = nextString(),
    firstName: String = nextString(),
    surname: String = nextString(),
    nickname: String? = nextString(),
    isBotUser: Boolean = nextBoolean(),
    isSurveyUser: Boolean = nextBoolean(),
    imageUrl: String = nextString(),
) = AgentModel(
    id = id,
    inContactId = inContactId,
    emailAddress = emailAddress,
    firstName = firstName,
    surname = surname,
    nickname = nickname,
    isBotUser = isBotUser,
    isSurveyUser = isSurveyUser,
    imageUrl = imageUrl
)
