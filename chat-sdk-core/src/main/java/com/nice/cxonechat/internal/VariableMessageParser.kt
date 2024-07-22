/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.internal

internal object VariableMessageParser {

    private const val OPENING_PARAM = "{{"
    private const val CLOSING_PARAM = "}}"
    private const val CUSTOMER_SEGMENT = "customer"
    private const val CONTACT_FIELD_SEGMENT = "contact.customFields"
    private const val CUSTOMER_FIELD_SEGMENT = "customer.customFields"
    private const val FALLBACK_MESSAGE = "fallbackMessage"

    @Suppress(
        "RegExpRedundantEscape" // Required on Android, otherwise it will cause runtime crash
    )
    private val VARIABLE_REGEX = """\{\{([^|]+?)(\|(.*?))?\}\}""".toRegex()
    private val INNER_KEY_REGEX = """(contact\.customFields|customer\.customFields|customer|)\.(\w*)""".toRegex()

    internal fun parse(
        message: String,
        parameters: Map<String, String>,
        customerFields: Map<String, String>,
        contactFields: Map<String, String>,
    ): String {
        if (!(message.contains(OPENING_PARAM) && message.contains(CLOSING_PARAM))) {
            return message
        }
        var useFallbackMessage = false
        var fallbackMessage: String? = null
        val messageWithFallback = message.replace(VARIABLE_REGEX) { result ->
            val (key, _, fallback) = result.destructured
            if (key == FALLBACK_MESSAGE) {
                fallbackMessage = fallback
                ""
            } else {
                val (replacement, needFallback) = replaceVariable(result, parameters, customerFields, contactFields)
                useFallbackMessage = useFallbackMessage || needFallback
                replacement
            }
        }
        val generalFallbackMessage = fallbackMessage
        return if (useFallbackMessage && generalFallbackMessage != null) {
            generalFallbackMessage
        } else {
            messageWithFallback
        }
    }

    private fun replaceVariable(
        result: MatchResult,
        parameters: Map<String, String>,
        customerFields: Map<String, String>,
        contactFields: Map<String, String>,
    ): Pair<String, Boolean> {
        val (key, fallbackGroup, rawFallback) = result.destructured
        val original = result.value
        val fallback = rawFallback.takeUnless { fallbackGroup.isEmpty() }.orOriginal(original)
        val keyResult = INNER_KEY_REGEX.find(key) ?: return fallback
        val (keyPrefix, innerKey) = keyResult.destructured
        return when (keyPrefix) {
            CUSTOMER_FIELD_SEGMENT -> customerFields[innerKey].orFallback(fallback)
            CONTACT_FIELD_SEGMENT -> contactFields[innerKey].orFallback(fallback)
            CUSTOMER_SEGMENT -> parameters[innerKey].orFallback(fallback)
            else -> fallback
        }
    }

    private fun String?.orFallback(fallback: Pair<String, Boolean>): Pair<String, Boolean> {
        return if (this != null) {
            this to false
        } else {
            fallback
        }
    }

    private fun String?.orOriginal(original: String): Pair<String, Boolean> {
        return if (this != null) {
            this to false
        } else {
            original to true
        }
    }
}
