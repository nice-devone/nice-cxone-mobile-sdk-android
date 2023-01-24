package com.nice.cxonechat.thread

import com.nice.cxonechat.Public
import java.util.UUID

/**
 * Represents all info about an agent.
 */
@Public
abstract class Agent {
    /** The id of the agent. */
    abstract val id: Int

    /** The id of the agent in the inContact (CXone) system. */
    abstract val inContactId: UUID? // todo find out why is this nullable

    /** The email address of the agent. */
    abstract val emailAddress: String? // todo find out why is this nullable

    /** The first name of the agent. */
    abstract val firstName: String

    /** The surname of the agent. */
    abstract val lastName: String

    /** The nickname of the agent. */
    abstract val nickname: String? // todo find out why is this nullable or necessary

    /** Whether the agent is a bot. */
    abstract val isBotUser: Boolean

    /** Whether the agent is for automated surveys. */
    abstract val isSurveyUser: Boolean

    /** The URL for the profile photo of the agent. */
    abstract val imageUrl: String

    /** Indicates that agent is currently typing */
    abstract val isTyping: Boolean

    /** The full name of the agent (readonly). */
    val fullName: String
        get() = "$firstName $lastName".trim()

}
