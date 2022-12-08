package com.nice.cxonechat.api

import com.nice.cxonechat.api.model.AttachmentUploadResponse
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.ChannelConfiguration
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

}
