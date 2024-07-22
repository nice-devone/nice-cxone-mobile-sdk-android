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

@file:Suppress("FunctionMaxLength", "StringLiteralDuplication")

package com.nice.cxonechat

import com.nice.cxonechat.internal.VariableMessageParser
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.nextStringMap
import com.nice.cxonechat.tool.nextStringPair
import org.junit.Test
import kotlin.test.assertEquals

internal class VariableMessageParserTest {

    @Test
    fun parse_message_without_variables() {
        val expected = "Welcome, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = expected,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_with_parameters() {
        val parameters = mapOf("firstName" to nextString())
        val message = "Welcome {{customer.firstName}}, how was your day?"
        val expected = "Welcome ${parameters["firstName"]}, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = parameters,
                customerFields = nextStringMap(),
                contactFields = nextStringMap()
            )
        )
    }

    @Test
    fun parser_message_with_parameterFallback() {
        val fallback = nextString()
        val parameters = mapOf("firstName" to nextString())
        val message = "Welcome {{customer.newVariable|$fallback}}, how was your day?"
        val expected = "Welcome $fallback, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = parameters,
                customerFields = nextStringMap(),
                contactFields = nextStringMap()
            )
        )
    }

    @Test
    fun parser_message_with_parameterEmptyFallback() {
        val parameters = mapOf("firstName" to nextString())
        val message = "Welcome{{customer.newValue|}} how was your day?"
        val expected = "Welcome how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = parameters,
                customerFields = nextStringMap(),
                contactFields = nextStringMap()
            )
        )
    }

    @Test
    fun parse_message_with_customerCustomFields() {
        val pair = nextStringPair()
        val message = "Welcome {{customer.customFields.${pair.first}}}, how was your day?"
        val expected = "Welcome ${pair.second}, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = mapOf(pair),
                contactFields = nextStringMap()
            )
        )
    }

    @Test
    fun parser_message_with_customerCustomFieldsFallback() {
        val fallback = nextString()
        val pair = nextStringPair()
        val message = "Welcome {{customer.customFields.newVariable|$fallback}}, how was your day?"
        val expected = "Welcome $fallback, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = mapOf(pair),
                contactFields = nextStringMap()
            )
        )
    }

    @Test
    fun parse_message_with_contactCustomFields() {
        val pair = nextStringPair()
        val message = "Welcome {{contact.customFields.${pair.first}}}, how was your day?"
        val expected = "Welcome ${pair.second}, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = mapOf(pair)
            )
        )
    }

    @Test
    fun parser_message_with_contactCustomFieldsFallback() {
        val fallback = nextString()
        val pair = nextStringPair()
        val message = "Welcome {{contact.customFields.newField|$fallback}}, how was your day?"
        val expected = "Welcome $fallback, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = mapOf(pair)
            )
        )
    }

    @Test
    fun parse_message_without_valid_variable() {
        val message = "Welcome {{unknown}}, how was your day?"
        val expected = "Welcome {{unknown}}, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_without_valid_variable_andEmptyFallback() {
        val message = "Welcome{{unknown|}} how was your day?"
        val expected = "Welcome how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_without_valid_variableWithFallback() {
        val fallback = nextString()
        val message = "Welcome {{unknown|$fallback}}, how was your day?"
        val expected = "Welcome $fallback, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_without_valid_variableWithGeneralFallback() {
        val fallback = nextString()
        val message = "Welcome {{unknown}}, how was your day?{{fallbackMessage|$fallback}}"
        assertEquals(
            fallback,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_with_missing_leading_bracket() {
        val message = "Welcome unknown}}, how was your day?"
        val expected = "Welcome unknown}}, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_message_with_missing_closing_bracket() {
        val message = "Welcome {{unknown, how was your day?"
        val expected = "Welcome {{unknown, how was your day?"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = nextStringMap(),
                customerFields = nextStringMap(),
                contactFields = nextStringMap(),
            )
        )
    }

    @Test
    fun parse_complex_message() {
        val name = nextString()
        val parameters = mapOf("firstName" to name)
        val customer = nextStringPair()
        val contact = nextStringPair()
        val message = "Welcome {{customer.firstName}}, " +
            "this is {{unknown|fallback}} personalized message " +
            "with {{contact.customFields.${contact.first}}} " +
            "and {{customer.customFields.${customer.first}}}"
        val expected = "Welcome $name, " +
            "this is fallback personalized message " +
            "with ${contact.second} " +
            "and ${customer.second}"
        assertEquals(
            expected,
            VariableMessageParser.parse(
                message = message,
                parameters = parameters,
                customerFields = mapOf(customer),
                contactFields = mapOf(contact),
            )
        )
    }
}
