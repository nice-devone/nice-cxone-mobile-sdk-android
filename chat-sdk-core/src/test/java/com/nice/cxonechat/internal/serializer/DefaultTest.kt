@file:Suppress("unused")

package com.nice.cxonechat.internal.serializer

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.tool.serialize
import org.junit.Test
import kotlin.test.assertEquals

internal class DefaultTest {

    /**
     * Verifies fix for DE-53160.
     */
    @Test
    fun verify_lenient_UUID_parsing_for_empty_strings() {
        val expectedAgent = makeAgent(inContactId = null)
        val agent = object {
            val id = expectedAgent.id
            val inContactId = ""
            val emailAddress: String? = expectedAgent.emailAddress
            val firstName: String = expectedAgent.firstName
            val surname: String = expectedAgent.surname
            val nickname: String? = expectedAgent.nickname
            val isBotUser: Boolean = expectedAgent.isBotUser
            val isSurveyUser: Boolean = expectedAgent.isSurveyUser
            val imageUrl: String = expectedAgent.imageUrl
        }
        val serializedAgent = agent.serialize()
        assertEquals(expectedAgent, Default.serializer.fromJson(serializedAgent, AgentModel::class.java))
    }
}
