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
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private typealias UploadResponse = Response<AttachmentUploadResponse?>
private typealias UploadCall = Call<AttachmentUploadResponse?>
private typealias UploadCallback = Callback<AttachmentUploadResponse?>

internal class RemoteServiceCaching(
    private val origin: RemoteService,
) : RemoteService by origin {

    private val queue = mutableMapOf<DestinationIdentifier, AttachmentUploadResponse>()

    override fun uploadFile(
        body: AttachmentUploadModel,
        brandId: String,
        channelId: String,
    ): UploadCall {
        val destination = DestinationIdentifier(body.hashCode(), brandId, channelId)
        val cached = queue[destination]
        if (cached != null) {
            return CachedCall(cached)
        }

        @Suppress("JoinDeclarationAndAssignment")
        var call: UploadCall
        call = origin.uploadFile(body, brandId, channelId)
        call = CachingCall(destination, call)
        return call
    }

    data class DestinationIdentifier(
        val hash: Int,
        val brandId: String,
        val channelId: String,
    )

    class CachedCall(
        private val response: AttachmentUploadResponse,
    ) : UploadCall {

        override fun clone(): UploadCall = this

        override fun execute(): UploadResponse = Response.success(response)

        override fun enqueue(callback: UploadCallback) {
            callback.onResponse(this, execute())
        }

        override fun isExecuted(): Boolean = true

        override fun cancel() {
            /* no-op */
        }

        override fun isCanceled(): Boolean = false

        override fun request(): Request = Request.Builder().build()

        override fun timeout(): Timeout = Timeout.NONE
    }

    inner class CachingCall(
        private val destination: DestinationIdentifier,
        private val origin: UploadCall,
    ) : UploadCall by origin {

        override fun clone(): UploadCall = CachingCall(destination, origin.clone())

        override fun execute(): UploadResponse = origin.execute().cache()

        override fun enqueue(callback: UploadCallback) {
            origin.enqueue(object : UploadCallback by callback {
                override fun onResponse(
                    call: UploadCall,
                    response: UploadResponse,
                ) {
                    callback.onResponse(call, response.cache())
                }
            })
        }

        private fun UploadResponse.cache() = apply {
            val body = body()
            if (body != null) {
                synchronized(queue) {
                    queue[destination] = body
                }
            }
        }
    }
}
