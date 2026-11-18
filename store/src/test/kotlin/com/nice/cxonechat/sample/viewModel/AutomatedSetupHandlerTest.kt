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

package com.nice.cxonechat.sample.viewModel

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.sample.data.models.RequestedEnvironment
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.SdkConfigurationRequest
import com.nice.cxonechat.sample.data.models.SdkEnvironment
import com.nice.cxonechat.sample.data.models.StoreEnvironment
import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import com.nice.cxonechat.sample.data.repository.LaunchEnvironments
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Covers what happens when a launch request is refused.
 *
 * The invariant is that this handler never leaves its own dialog up once it stops driving: a refused
 * request must clear the spinner of a setup it tore down, and must publish nothing when there was no
 * setup to tear down, because the caller decides that case for itself.
 */
internal class AutomatedSetupHandlerTest {

    @Test
    fun `an unknown environment is refused without publishing a state when no setup is running`() = runBlocking<Unit> {
        val fixture = Fixture()

        fixture.handler.start(UNKNOWN_ENVIRONMENT_REQUEST, STORED_CONFIGURATION) shouldBe false

        fixture.handler.isActive shouldBe false
        // Publishing here would pre-empt the caller's own decision to ask for a configuration.
        fixture.published.shouldBeEmpty()
        fixture.presentation.isStorefrontVisible.value shouldBe true
    }

    @Test
    fun `an unknown environment refused while a setup runs clears the spinner it leaves behind`() = runBlocking<Unit> {
        val fixture = Fixture()
        fixture.handler.start(NO_CONFIGURATION_REQUEST, STORED_CONFIGURATION) shouldBe true
        fixture.handler.isActive shouldBe true

        fixture.handler.start(UNKNOWN_ENVIRONMENT_REQUEST, STORED_CONFIGURATION) shouldBe false

        fixture.handler.isActive shouldBe false
        // Left as AutomatedSetup, this is unrecoverable: BusySpinner ignores back and outside taps,
        // and its Cancel returns early once the setup has stopped.
        fixture.published.last() shouldBe UiState.Prepared
        fixture.presentation.isStorefrontVisible.value shouldBe true
    }

    @Test
    fun `a request supplying no configuration with none stored is refused without publishing a state`() = runBlocking<Unit> {
        val fixture = Fixture()

        fixture.handler.start(NO_CONFIGURATION_REQUEST, storedConfiguration = null) shouldBe false

        fixture.handler.isActive shouldBe false
        fixture.published.shouldBeEmpty()
        fixture.presentation.isStorefrontVisible.value shouldBe true
    }

    /**
     * A handler wired to collect what it publishes.
     *
     * The presentation starts with a request pending, as it does in the app, so that a restored
     * storefront is a state the refusal actually reached rather than the one it started in.
     */
    private class Fixture {
        val published = mutableListOf<UiState>()

        val presentation = LaunchPresentation(
            PendingLaunchConfiguration().apply { offer(NO_CONFIGURATION_REQUEST) }
        )

        val handler = AutomatedSetupHandler(
            chatProvider = mockk<ChatInstanceProvider>(relaxed = true).also {
                every { it.chatState } returns ChatState.Preparing
            },
            chatSettingsHandler = mockk(relaxed = true) {
                every { userName } returns null
            },
            launchEnvironments = NothingResolves,
            presentation = presentation,
            setUiState = published::add,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            logger = LoggerNoop,
        )
    }

    /** Stands in for a copy of the app which can resolve no environment by name. */
    private object NothingResolves : LaunchEnvironments {
        override val availableNames = emptyList<String>()

        override fun byName(name: String): SdkEnvironment? = null
    }

    private companion object {
        const val BRAND_ID = 1234L
        const val CHANNEL_ID = "chat_abc"

        val UNKNOWN_ENVIRONMENT_REQUEST = StoreLaunchRequest(
            configuration = SdkConfigurationRequest(
                environment = RequestedEnvironment.Named("NOT_A_KNOWN_ENVIRONMENT"),
                brandId = BRAND_ID,
                channelId = CHANNEL_ID,
            ),
            loginData = null,
            openChat = false,
        )

        /** Asks only for the stored channel to be opened, which is what keeps a refusal reachable. */
        val NO_CONFIGURATION_REQUEST = StoreLaunchRequest(
            configuration = null,
            loginData = null,
            openChat = false,
        )

        /** A production name, so no non-production environment is named even in test source. */
        val STORED_CONFIGURATION = SdkConfiguration(
            name = StoreEnvironment.PROD_EU1.name,
            environment = StoreEnvironment.PROD_EU1.sdkEnvironment,
            brandId = BRAND_ID,
            channelId = CHANNEL_ID,
        )
    }
}
