package com.nice.cxonechat.internal

internal object VariableMessageParser {

    private const val OPENING_PARAM = "{{"
    private const val CLOSING_PARAM = "}}"
    private const val CUSTOMER_SEGMENT = "customer."
    private const val CONTACT_FIELD_SEGMENT = "contact.customFields."
    private const val CUSTOMER_FIELD_SEGMENT = "customer.customFields."
    private val VARIABLE_REGEX = """\{\{([^|]+?)(\|(.*?))?}}""".toRegex()

    internal fun parse(
        message: String,
        parameters: Map<String, String>,
        customerFields: Map<String, String>,
        contactFields: Map<String, String>,
    ): String {
        if (!(message.contains(OPENING_PARAM) && message.contains(CLOSING_PARAM))) {
            return message
        }
        return message.replace(VARIABLE_REGEX) { result ->
            val (key, fallbackGroup, rawFallback) = result.destructured
            val fallback = rawFallback.takeUnless { fallbackGroup.isEmpty() } ?: result.value
            when {
                key.startsWith(CUSTOMER_FIELD_SEGMENT) -> customerFields[key.removePrefix(CUSTOMER_FIELD_SEGMENT)] ?: fallback
                key.startsWith(CONTACT_FIELD_SEGMENT) -> contactFields[key.removePrefix(CONTACT_FIELD_SEGMENT)] ?: fallback
                key.startsWith(CUSTOMER_SEGMENT) -> parameters[key.removePrefix(CUSTOMER_SEGMENT)] ?: fallback
                else -> fallback
            }
        }
    }
}
