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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.nice.cxonechat.enums.VisitorEventType.VisitorVisit
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.event.AnalyticsEvent.Destination
import com.nice.cxonechat.internal.RemoteServiceBuilder
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.AvailabilityStatus.Offline
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.internal.model.ChannelAvailability
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.tool.MockInterceptor
import io.kotest.matchers.shouldBe
import okhttp3.Protocol.HTTP_2
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.test.assertEquals

internal class RemoteServiceTest {
    private val recorder by lazy { MockInterceptor() }
    private val connection by lazy { makeConnection() }
    private val builder by lazy {
        RemoteServiceBuilder()
            .setConnection(connection)
            .setInterceptor(recorder)
    }
    private val baseUrl by lazy {
        connection.environment.chatUrl.replace("/chat/", "/")
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

    @Test
    fun getChannelAvailabilityOnline() {
        val client = builder.build()

        recorder.addResponse {
            code(200)
            message("")
            body("""{"status":"online"}""".toResponseBody())
        }

        client.getChannelAvailability(brandId = "BrandId", channelId = "ChannelId").execute().let { response ->
            response.body() shouldBe ChannelAvailability(status = Online)
        }
    }

    @Test
    fun getChannelAvailabilityOffline() {
        val client = builder.build()

        recorder.addResponse {
            code(200)
            message("")
            body("""{"status":"offline"}""".toResponseBody())
        }

        client.getChannelAvailability(brandId = "BrandId", channelId = "ChannelId").execute().let { response ->
            response.body() shouldBe ChannelAvailability(status = Offline)
        }
    }

    @Test
    fun testAvailabilityStatus() {
        Online.isOnline shouldBe true
        Offline.isOnline shouldBe false
    }

    @Test
    fun postEventContents() {
        val client = builder.build()

        recorder.addResponse {
            code(200)
            message("")
            body("".toResponseBody())
        }

        val expect = AnalyticsEvent(
            kEventId,
            VisitorVisit,
            kVisitId,
            Destination(kDestinationId),
            kNow,
            mapOf<String, String>()
        )

        client.postEvent(
            connection.brandId.toString(),
            connection.visitorId.toString(),
            expect
        ).execute()

        assertEquals(1, recorder.requests.count())
        val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'z'"
        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
            .setDateFormat(dateFormat)
            .setLenient()
            .create()

        with(recorder.requests.first()) {
            assertEquals("POST", method)
            assertEquals(
                "${baseUrl}web-analytics/1.0/tenants/${connection.brandId}/visitors/${connection.visitorId}/events",
                url.toString()
            )

            val actual = body?.asString?.let {
                gson.fromJson(it, AnalyticsEvent::class.java)
            }

            assertEquals(expect, actual)
        }
    }

    @Test
    fun upload_cachesIdenticalAttachments() {
        recorder.addResponse {
            protocol(HTTP_2)
            code(200)
            message("")
            body("""{"fileUrl":"fileUrl"}""".toResponseBody())
        }

        val client = builder.build()
        val upload = AttachmentUploadModel("content", "mime", "name.txt")
        client.uploadFile(upload, "0", "channelId").execute()
        client.uploadFile(upload, "0", "channelId").execute()

        assertEquals(1, recorder.requests.count())
    }

    companion object {
        val kEventId = UUID.randomUUID()
        val kVisitId = UUID.randomUUID()
        val kDestinationId = UUID.randomUUID()
        val kNow = Date()
    }
}

internal class GsonUTCDateAdapter : JsonSerializer<Date?>, JsonDeserializer<Date?> {
    private val dateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun serialize(date: Date) = dateFormat.format(date)

    override fun serialize(date: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return date?.let { JsonPrimitive(dateFormat.format(date)) }!!
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        return try {
            json?.asString?.let(dateFormat::parse)
        } catch (e: ParseException) {
            throw JsonParseException(e)
        }
    }
}
