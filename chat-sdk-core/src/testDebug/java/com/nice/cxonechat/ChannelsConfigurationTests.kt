/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.AvailabilityStatus.Offline
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.state.Configuration.Feature.LiveChatLogoHidden
import com.nice.cxonechat.state.Configuration.Feature.ProactiveChatEnabled
import com.nice.cxonechat.state.Configuration.Feature.RecoverLiveChatDoesNotFail
import com.nice.cxonechat.state.FieldDefinition.Hierarchy
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class ChannelsConfigurationTests {
    private val channelConfigurationData: String by lazy {
        requireNotNull(ResourceHelper.loadString("channelconfiguration.json"))
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun configuration(
        isLiveChat: Boolean = false,
        isOnline: Boolean = true,
        channelId: String? = null,
    ) = json.decodeFromString<ChannelConfiguration>(channelConfigurationData).let {
        it.copy(
            isLiveChat = isLiveChat,
            availability = it.availability.copy(status = if (isOnline) Online else Offline),
            preContactForm = it.preContactForm?.let { preContactFormModel ->
                if (channelId != null) {
                    preContactFormModel.copy(channels = preContactFormModel.channels + listOf(ChannelIdentifier(channelId)))
                } else preContactFormModel
            }
        )
    }

    @Test
    fun testLiveChatRelatedParsing() {
        listOf(
            true to true,
            true to false,
            false to true,
            false to false
        ).forEach { (isLiveChat, isOnline) ->
            val configuration = configuration(isLiveChat = isLiveChat, isOnline = isOnline)

            assertEquals(isLiveChat, configuration.isLiveChat)
            assertEquals(if (isOnline) Online else Offline, configuration.availability.status)
        }
    }

    @Test
    fun testParsing() {
        val configuration = configuration()

        with(configuration.settings.fileRestrictions) {
            assertEquals(40, allowedFileSize)
            assertEquals(11, allowedFileTypes.size)
            assertTrue(isAttachmentsEnabled)
        }
        assertFalse(configuration.isLiveChat)
        assertEquals(Online, configuration.availability.status)
        with(configuration.settings.features) {
            assertEquals(this["liveChatLogoHidden"], false)
            assertEquals(this["isProactiveChatEnabled"], true)
        }
    }

    @Test
    fun testPublication() {
        val published = configuration().toConfiguration(channelId)

        with(published.fileRestrictions) {
            assertEquals(40, allowedFileSize)
            assertEquals(10, allowedFileTypes.size)
            assertTrue(isAttachmentsEnabled)
        }
    }

    @Test
    fun testHasFeature() {
        val published = configuration().toConfiguration(channelId)

        // This is false in the test data.
        assertFalse(published.hasFeature("liveChatLogoHidden"))
        assertFalse(published.hasFeature(LiveChatLogoHidden))

        // This is true in the test data.
        assertTrue(published.hasFeature("isProactiveChatEnabled"))
        assertTrue(published.hasFeature(ProactiveChatEnabled))

        // This is missing in the test data and should default to true.
        assertTrue(published.hasFeature("isRecoverLivechatDoesNotFailEnabled"))
        assertTrue(published.hasFeature(RecoverLiveChatDoesNotFail))
    }

    @Suppress("NestedBlockDepth") // verifying hierarchic data just looks better with nested when
    @Test
    fun testHierarchicPublication() {
        val published = configuration(channelId = channelId).toConfiguration(channelId)
        val preContactSurvey = requireNotNull(published.preContactSurvey)
        val hier = preContactSurvey.fields.firstOrNull { it.fieldId == "hie2" } as Hierarchy

        with(hier.values.toList()) {
            assertEquals(2, size)

            with(get(0)) {
                assertEquals("0", label)
                assertEquals(1, children.count())
                with(children.toList()[0]) {
                    assertEquals("0-0", label)
                    assertEquals(2, children.count())
                    with(children.toList()[0]) {
                        assertEquals("0-0-0", label)
                        assertEquals(2, children.count())
                        with(children.toList()[0]) {
                            assertEquals("0-0-0-0", label)
                            assertEquals(0, children.count())
                        }
                        with(children.toList()[1]) {
                            assertEquals("0-0-0-1", label)
                            assertEquals(0, children.count())
                        }
                    }
                    with(children.toList()[1]) {
                        assertEquals("0-0-1", label)
                        assertEquals(0, children.count())
                    }
                }
            }

            with(get(1)) {
                assertEquals("1", label)
                assertEquals(0, children.count())
            }
        }
    }

    companion object {
        const val channelId = ">>Channel Id<<"
    }
}
