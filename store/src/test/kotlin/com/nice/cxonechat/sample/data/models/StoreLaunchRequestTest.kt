/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample.data.models

import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Malformed
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.None
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Supplied
import com.nice.cxonechat.sample.utilities.ExtrasSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Verifies that launch intents are read strictly: a well-formed command produces exactly the
 * requested channel, and anything ambiguous or incomplete is rejected with a reason rather than
 * partially applied.
 *
 * Environment *names* are deliberately not validated here -- they are looked up later, once the
 * bundled definitions have loaded, because the intent is read before that happens.
 */
internal class StoreLaunchRequestTest {

    @Test
    fun `an intent without any launch extras asks for nothing`() {
        parse() shouldBe None
    }

    @Test
    fun `unrelated extras are ignored`() {
        parse("someOtherApp.EXTRA" to "value") shouldBe None
    }

    @Test
    fun `a named environment is carried through for later resolution`() {
        val result = parse(namedEnvironment()).supplied()

        result.configuration shouldBe SdkConfigurationRequest(
            environment = RequestedEnvironment.Named(ENVIRONMENT_NAME),
            brandId = BRAND_ID,
            channelId = CHANNEL_ID,
        )
    }

    @Test
    fun `an unrecognised environment name is left for resolution rather than rejected`() {
        // Nothing here knows which names exist; a copy without the bundled definitions has fewer of
        // them, so the check belongs where the definitions are.
        val result = parse(
            LaunchExtras.ENVIRONMENT to "NOT_AN_ENVIRONMENT",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).supplied()

        result.configuration?.environment shouldBe RequestedEnvironment.Named("NOT_AN_ENVIRONMENT")
    }

    @Test
    fun `a requested channel is named after the environment it resolved to`() {
        val request = SdkConfigurationRequest(
            environment = RequestedEnvironment.Named(ENVIRONMENT_NAME),
            brandId = BRAND_ID,
            channelId = CHANNEL_ID,
        )
        val resolved = StoreEnvironment.PROD_EU1.sdkEnvironment

        request.toSdkConfiguration(resolved) shouldBe SdkConfiguration(
            name = resolved.name,
            environment = resolved,
            brandId = BRAND_ID,
            channelId = CHANNEL_ID,
        )
    }

    @Test
    fun `a brand id supplied as an int rather than a long is rejected`() {
        // `adb shell am start --ei brandId 1234` stores an int; reading it as a long would silently
        // yield the default and connect to the wrong brand.
        parse(
            LaunchExtras.ENVIRONMENT to ENVIRONMENT_NAME,
            LaunchExtras.BRAND_ID to BRAND_ID.toInt(),
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe "'brandId' must be supplied as a long (--el)"
    }

    @Test
    fun `a mistyped extra is rejected even when it is the only one supplied`() {
        parse(LaunchExtras.BRAND_ID to BRAND_ID.toInt()).reason() shouldBe
                "'brandId' must be supplied as a long (--el)"
    }

    @Test
    fun `an open chat flag supplied as a string is rejected rather than defaulted`() {
        parse(LaunchExtras.OPEN_CHAT to "false").reason() shouldBe
                "'openChat' must be supplied as a boolean (--ez)"
    }

    @Test
    fun `an environment supplied as an int is rejected`() {
        parse(LaunchExtras.ENVIRONMENT to 1).reason() shouldBe
                "'environment' must be supplied as a string (--es)"
    }

    @Test
    fun `a brand and channel without an environment are rejected rather than ignored`() {
        parse(
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe
                "'environment' is required alongside 'brandId', 'channelId' and the custom environment extras"
    }

    @Test
    fun `an environment without a channel id is rejected`() {
        parse(
            LaunchExtras.ENVIRONMENT to ENVIRONMENT_NAME,
            LaunchExtras.BRAND_ID to BRAND_ID,
        ).reason() shouldBe "'environment' also requires 'channelId'"
    }

    @Test
    fun `a custom environment is derived from a host stem`() {
        val result = parse(customByHost()).supplied()

        result.configuration?.environment shouldBe RequestedEnvironment.Custom(
            sdkEnvironmentFromHost("de-na1.niceincontact.com")
        )
    }

    @Test
    fun `a custom environment may instead be described url by url`() {
        val result = parse(customByUrls()).supplied()

        result.configuration?.environment shouldBe RequestedEnvironment.Custom(
            SdkEnvironment(
                name = "Local",
                location = "Europe",
                baseUrl = "https://base/",
                socketUrl = "wss://socket",
                originHeader = "https://origin",
                chatUrl = "https://base/chat/",
                loggerUrl = "",
                tokenUrl = "https://token/",
            )
        )
    }

    @Test
    fun `a host stem carrying a scheme is rejected rather than concatenated into the urls`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.ENVIRONMENT_HOST to "https://de-na1.niceincontact.com",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe HOST_STEM_REASON
    }

    @Test
    fun `a host stem with a trailing slash is rejected`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.ENVIRONMENT_HOST to "de-na1.niceincontact.com/",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe HOST_STEM_REASON
    }

    @Test
    fun `an empty host stem is rejected rather than building prefix-only urls`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.ENVIRONMENT_HOST to "  ",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe HOST_STEM_REASON
    }

    @Test
    fun `a host stem carrying a path is rejected`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.ENVIRONMENT_HOST to "de-na1.niceincontact.com/chat",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe HOST_STEM_REASON
    }

    @Test
    fun `a host stem containing a space from a mis-quoted intent is rejected`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.ENVIRONMENT_HOST to "de-na1 niceincontact.com",
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe HOST_STEM_REASON
    }

    @Test
    fun `supplying both a host stem and individual urls is rejected as ambiguous`() {
        parse(customByHost() + customByUrls().filterNot { it.first == LaunchExtras.ENVIRONMENT }).reason() shouldBe
                "supply either 'environmentHost' or the individual environment urls, not both"
    }

    @Test
    fun `a custom environment describing itself neither way is rejected`() {
        parse(
            LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
            LaunchExtras.BRAND_ID to BRAND_ID,
            LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        ).reason() shouldBe "'environment=CUSTOM' requires either 'environmentHost' or the individual urls"
    }

    @Test
    fun `an incomplete set of individual urls is rejected`() {
        parse(customByUrls().filterNot { it.first == LaunchExtras.SOCKET_URL })
            .shouldBeInstanceOf<Malformed>()
    }

    @Test
    fun `a first and last name sign the user in`() {
        val result = parse(
            namedEnvironment() + listOf(
                LaunchExtras.FIRST_NAME to "QA",
                LaunchExtras.LAST_NAME to "Automation",
                LaunchExtras.CUSTOMER_ID to "customer-1",
            )
        ).supplied()

        result.loginData shouldBe LoginData(
            userName = ChatUserName(lastName = "Automation", firstName = "QA"),
            customerId = "customer-1",
        )
    }

    @Test
    fun `a first name without a last name is rejected`() {
        parse(LaunchExtras.FIRST_NAME to "QA").reason() shouldBe
                "'firstName' and 'lastName' must be supplied together"
    }

    @Test
    fun `a channel supplied without a name signs in a stand-in user`() {
        val result = parse(namedEnvironment()).supplied()

        result.loginData shouldBe LoginData(
            userName = ChatUserName(
                lastName = DefaultChatUser.LAST_NAME,
                firstName = DefaultChatUser.FIRST_NAME,
            ),
            customerId = null,
        )
    }

    @Test
    fun `a customer id is kept when the name is left to the stand-in`() {
        val result = parse(namedEnvironment() + (LaunchExtras.CUSTOMER_ID to "customer-1")).supplied()

        result.loginData?.customerId shouldBe "customer-1"
        result.loginData?.userName?.firstName shouldBe DefaultChatUser.FIRST_NAME
    }

    @Test
    fun `no stand-in user is invented when the intent supplies no channel`() {
        parse(LaunchExtras.OPEN_CHAT to true).supplied().loginData shouldBe null
    }

    @Test
    fun `a customer id with nothing to attach it to is rejected`() {
        parse(LaunchExtras.CUSTOMER_ID to "customer-1").reason() shouldBe
                "'customerId' also requires 'firstName' and 'lastName', or an 'environment' to sign in to"
    }

    @Test
    fun `the chat is opened unless the intent opts out`() {
        parse(namedEnvironment()).supplied().openChat shouldBe true
        parse(namedEnvironment() + (LaunchExtras.OPEN_CHAT to true)).supplied().openChat shouldBe true
        parse(namedEnvironment() + (LaunchExtras.OPEN_CHAT to false)).supplied().openChat shouldBe false
    }

    @Test
    fun `opening the chat may be requested without any configuration`() {
        val result = parse(LaunchExtras.OPEN_CHAT to true).supplied()

        result.configuration shouldBe null
        result.openChat shouldBe true
    }

    private fun parse(vararg extras: Pair<String, Any>) = parse(extras.toList())

    private fun parse(extras: List<Pair<String, Any>>) = parseStoreLaunchRequest(MapExtrasSource(extras.toMap()))

    private fun StoreLaunchRequestResult.supplied() = shouldBeInstanceOf<Supplied>().request

    private fun StoreLaunchRequestResult.reason() = shouldBeInstanceOf<Malformed>().reason

    private fun namedEnvironment() = listOf<Pair<String, Any>>(
        LaunchExtras.ENVIRONMENT to ENVIRONMENT_NAME,
        LaunchExtras.BRAND_ID to BRAND_ID,
        LaunchExtras.CHANNEL_ID to CHANNEL_ID,
    )

    private fun customByHost() = listOf<Pair<String, Any>>(
        LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
        LaunchExtras.ENVIRONMENT_HOST to "de-na1.niceincontact.com",
        LaunchExtras.BRAND_ID to BRAND_ID,
        LaunchExtras.CHANNEL_ID to CHANNEL_ID,
    )

    private fun customByUrls() = listOf<Pair<String, Any>>(
        LaunchExtras.ENVIRONMENT to StoreEnvironment.CUSTOM,
        LaunchExtras.BRAND_ID to BRAND_ID,
        LaunchExtras.CHANNEL_ID to CHANNEL_ID,
        LaunchExtras.ENV_NAME to "Local",
        LaunchExtras.ENV_LOCATION to "Europe",
        LaunchExtras.BASE_URL to "https://base/",
        LaunchExtras.SOCKET_URL to "wss://socket",
        LaunchExtras.ORIGIN_HEADER to "https://origin",
        LaunchExtras.CHAT_URL to "https://base/chat/",
        LaunchExtras.TOKEN_URL to "https://token/",
    )

    private companion object {
        /** A production name, so no non-production environment is named even in test source. */
        const val ENVIRONMENT_NAME = "PROD_EU1"
        const val BRAND_ID = 1234L
        const val CHANNEL_ID = "chat_abc"
        const val HOST_STEM_REASON =
            "'environmentHost' is a host stem such as 'de-na1.example.com', without scheme, path or trailing slash"
    }

    /**
     * Stands in for intent extras. Storing the values as [Any] lets a test supply a wrongly-typed
     * value, mirroring what `adb shell am start --ei` would put in a real intent.
     */
    private class MapExtrasSource(private val values: Map<String, Any>) : ExtrasSource {
        override fun has(key: String) = values.containsKey(key)

        override fun string(key: String) = values[key] as? String

        override fun long(key: String) = values[key] as? Long

        override fun boolean(key: String) = values[key] as? Boolean
    }
}
