package com.nice.cxonechat.prechat

import com.nice.cxonechat.state.FieldDefinitionList

internal data class PreChatSurveyInternal(
    override val name: String,
    override val fields: FieldDefinitionList,
) : PreChatSurvey {
    override fun toString(): String =
        "PreChatSurvey(" +
            "name='$name'" +
            ", fields=$fields" +
            ")"
}
