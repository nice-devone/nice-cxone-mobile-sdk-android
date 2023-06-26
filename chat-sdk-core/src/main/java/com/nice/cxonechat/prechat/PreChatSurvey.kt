package com.nice.cxonechat.prechat

import com.nice.cxonechat.Public
import com.nice.cxonechat.state.FieldDefinitionList

/**
 * A definition of the pre-chat form which should be answered by the user before the new thread is created.
 */
@Public
sealed interface PreChatSurvey {

    /**
     * Name of the dynamic pre-chat survey.
     */
    val name: String

    /**
     * Sequence of pre-chat survey fields which should be presented to user before chat thread is created.
     */
    val fields: FieldDefinitionList
}
