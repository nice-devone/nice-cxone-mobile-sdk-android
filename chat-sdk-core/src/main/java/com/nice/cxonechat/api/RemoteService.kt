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

package com.nice.cxonechat.api

import com.nice.cxonechat.api.model.AttachmentUploadResponse
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.ChannelAvailability
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.Visitor
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface RemoteService {

    @POST("1.0/brand/{brandId}/channel/{channelId}/attachment")
    fun uploadFile(
        @Body
        body: AttachmentUploadModel,
        @Path("brandId")
        brandId: String,
        @Path("channelId")
        channelId: String,
    ): Call<AttachmentUploadResponse?>

    @GET("1.0/brand/{brandId}/channel/{channelId}")
    fun getChannel(
        @Path("brandId")
        brandId: String,
        @Path("channelId")
        channelId: String,
    ): Call<ChannelConfiguration?>

    @GET("1.0/brand/{brandId}/channel/{channelId}/availability")
    fun getChannelAvailability(
        @Path("brandId")
        brandId: String,
        @Path("channelId")
        channelId: String
    ): Call<ChannelAvailability>

    @POST("/web-analytics/1.0/tenants/{brandId}/visitors/{visitorId}/events")
    fun postEvent(
        @Path("brandId")
        brandId: String,
        @Path("visitorId")
        visitorId: String,
        @Body
        event: AnalyticsEvent,
    ): Call<Void>

    @PUT("/web-analytics/1.0/tenants/{brandId}/visitors/{visitorId}")
    fun createOrUpdateVisitor(
        @Path("brandId")
        brandId: Int,
        @Path("visitorId")
        visitorId: String,
        @Body
        visitor: Visitor,
    ): Call<Void>
}
