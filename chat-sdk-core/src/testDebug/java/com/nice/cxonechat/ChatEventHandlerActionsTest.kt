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

package com.nice.cxonechat

import com.google.gson.Strictness
import com.nice.cxonechat.ChatEventHandlerActions.conversion
import com.nice.cxonechat.ChatEventHandlerActions.pageView
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionClick
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionDisplay
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionFailure
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionSuccess
import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.api.GsonUTCDateAdapter
import com.nice.cxonechat.enums.ActionType.WelcomeMessage
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.enums.VisitorEventType.Conversion
import com.nice.cxonechat.enums.VisitorEventType.PageView
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionClicked
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionDisplayed
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionFailed
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionSuccess
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.event.AnalyticsEvent.Data
import com.nice.cxonechat.event.AnalyticsEvent.Data.ValueMapData
import com.nice.cxonechat.event.AnalyticsEvent.Destination
import com.nice.cxonechat.internal.ChatEventHandlerImpl
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.RemoteServiceBuilder
import com.nice.cxonechat.internal.model.network.PageViewData
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.Gson
import com.nice.cxonechat.tool.MockInterceptor
import com.nice.cxonechat.tool.awaitResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import com.nice.cxonechat.internal.model.network.Conversion as ConversionModel

internal class ChatEventHandlerActionsTest {
    private val gson by lazy {
        Gson.newBuilder()
            .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
            .setStrictness(Strictness.LENIENT)
            .create()
    }
    private val visitorId = UUID.randomUUID()
    private val customerId = UUID.randomUUID().toString()
    private val destinationId = UUID.randomUUID()
    private val visitId = UUID.randomUUID()
    private val eventId = UUID.randomUUID()
    private val brandId = Random.nextInt()
    private val now = Date()
    private val interceptor by lazy { MockInterceptor() }
    private val httpClient by lazy { OkHttpClient().newBuilder().addInterceptor(interceptor).build() }
    private val baseUrl = "https://chat.server/"
    private val chatUrl by lazy { "${baseUrl}chat/" }
    private val analyticsUrl by lazy { "${baseUrl}web-analytics/1.0/tenants/$brandId/visitors/$visitorId/events" }
    private val mockEnvironment by lazy {
        mockk<Environment> {
            every { chatUrl } returns this@ChatEventHandlerActionsTest.chatUrl
        }
    }
    private val mockConnection by lazy {
        mockk<Connection> {
            every { environment } returns mockEnvironment
            every { brandId } returns this@ChatEventHandlerActionsTest.brandId
        }
    }
    private val mockStorage by lazy {
        val local = this
        mockk<ValueStorage> {
            every { visitorId } returns local.visitorId
            every { customerId } returns local.customerId
            every { destinationId } returns local.destinationId
            every { welcomeMessage } returns "welcome"
            every { authToken } returns "token"
            every { visitId } returns local.visitId
        }
    }
    private val mockService by lazy {
        RemoteServiceBuilder()
            .setConnection(mockConnection)
            .setSharedOkHttpClient(httpClient)
            .build()
    }
    private val mockChat by lazy {
        mockk<ChatWithParameters> {
            every { service } returns mockService
            every { connection } returns mockConnection
            every { storage } returns mockStorage
        }
    }
    private val events by lazy {
        ChatEventHandlerImpl(mockChat)
    }

    private val RequestBody.asString: String
        get() {
            return Buffer().use { buffer ->
                writeTo(buffer)
                ByteArrayOutputStream().use { baos ->
                    buffer.writeTo(baos)
                    baos
                }.toString()
            }
        }

    private val kActionId = UUID.randomUUID()
    private val actionMetaData = ActionMetadataInternal(
        kActionId,
        "action name",
        WelcomeMessage
    )

    private fun verifyEventSent(expect: AnalyticsEvent, send: ChatEventHandler.(done: () -> Unit) -> Unit) {
        awaitResult(200.milliseconds) { done ->
            events.send { done(Unit) }
        }
        assertEquals(1, interceptor.requests.size)
        with(interceptor.requests.first()) {
            assertEquals("POST", method)
            assertEquals(analyticsUrl, url.toString())

            val actual = gson.fromJson(this.body!!.asString, AnalyticsEvent::class.java)

            assertEquals(
                expect.copy(eventId = actual.eventId),
                actual
            )
        }
    }

    private fun event(type: VisitorEventType, data: Data = ValueMapData(mapOf())): AnalyticsEvent {
        return AnalyticsEvent(
            eventId,
            type,
            visitId,
            Destination(destinationId),
            now,
            data,
        )
    }

    @Test
    fun conversion() {
        val expect = event(
            Conversion,
            Data.ConversionData(ConversionModel("cash", 324, now))
        )
        verifyEventSent(expect) { done ->
            events.conversion("cash", 324, now) { done() }
        }
    }

    @Test
    fun pageView() {
        val expect = event(
            PageView,
            Data.PageViewData(PageViewData("some title", "https://some.url/or/other"))
        )
        verifyEventSent(expect) { done ->
            events.pageView("some title", "https://some.url/or/other", now) { done() }
        }
    }

    @Test
    fun proactiveActionClick() {
        val expect = event(ProactiveActionClicked, Data.ProactiveActionData(ProactiveActionInfo(actionMetaData)))
        verifyEventSent(expect) { done ->
            events.proactiveActionClick(actionMetaData, now) { done() }
        }
    }

    @Test
    fun proactiveActionDisplay() {
        val expect = event(ProactiveActionDisplayed, Data.ProactiveActionData(ProactiveActionInfo(actionMetaData)))
        verifyEventSent(expect) { done ->
            events.proactiveActionDisplay(actionMetaData, now) { done() }
        }
    }

    @Test
    fun proactiveActionFailure() {
        val expect = event(ProactiveActionFailed, Data.ProactiveActionData(ProactiveActionInfo(actionMetaData)))
        verifyEventSent(expect) { done ->
            events.proactiveActionFailure(actionMetaData, now) { done() }
        }
    }

    @Test
    fun proactiveActionSuccess() {
        val expect = event(ProactiveActionSuccess, Data.ProactiveActionData(ProactiveActionInfo(actionMetaData)))
        verifyEventSent(expect) { done ->
            events.proactiveActionSuccess(actionMetaData, now) { done() }
        }
    }
}
