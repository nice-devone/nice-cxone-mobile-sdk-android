package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * The various options for how a channel is configured.
 */
@Public
abstract class Configuration {

    /** Whether the channel supports multiple threads for the same user. */
    abstract val hasMultipleThreadsPerEndUser: Boolean

    /** Whether the channel supports proactive chat features. */
    abstract val isProactiveChatEnabled: Boolean

    /** Whether OAuth authorization is enabled for the channel. */
    abstract val isAuthorizationEnabled: Boolean

    /**
     * Custom fields defined for supplying of additional information about customer,
     * for example data supplied during a pre-chat survey.
     */
    abstract val contactCustomFields: FieldDefinitionList

    /**
     * Definition of possible custom fields which are usable/valid for all
     * contacts with the customer.
     */
    abstract val customerCustomFields: FieldDefinitionList

    /** Return the list of all available customer fields. */
    val allCustomFields: FieldDefinitionList
        get() = contactCustomFields + customerCustomFields

    /**
     * Check if a given field ID is allowed by the receiving [Configuration].
     *
     * @param fieldId Field ID to check for validity.
     * @return Returns true iff [fieldId] is valid with the current configuration, i.e.,
     * is included in either [contactCustomFields] or [customerCustomFields].
     */
    fun allowsFieldId(fieldId: String): Boolean =
        allCustomFields.containsField(fieldId)
}
