package com.nice.cxonechat.thread

import com.nice.cxonechat.Public

/**
 * Represents all data about a single custom field. Please note that the
 * implementations that use this field can use distinctions by [id] to
 * pick only a singular value.
 *
 * Never create or pass same [id]s with different values if you don't
 * want the latest values, it can lead to unexpected consequences.
 *
 * Consult the documentation for given implementation.
 * */
@Public
abstract class CustomField {
    /**
     * Identifier or name of given property in custom field.
     * Consult a representative for more information.
     * */
    abstract val id: String

    /**
     * Value for given identifier.
     * */
    abstract val value: String

}
