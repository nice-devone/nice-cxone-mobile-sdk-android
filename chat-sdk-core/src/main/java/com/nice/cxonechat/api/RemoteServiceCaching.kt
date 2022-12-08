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
        if (cached != null)
            return CachedCall(cached)

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

        override fun clone(): UploadCall {
            return this
        }

        override fun execute(): UploadResponse {
            return Response.success(response)
        }

        override fun enqueue(callback: UploadCallback) {
            callback.onResponse(this, execute())
        }

        override fun isExecuted(): Boolean {
            return true
        }

        override fun cancel() {
            /* no-op */
        }

        override fun isCanceled(): Boolean {
            return false
        }

        override fun request(): Request {
            return Request.Builder().build()
        }

        override fun timeout(): Timeout {
            return Timeout.NONE
        }

    }

    inner class CachingCall(
        private val destination: DestinationIdentifier,
        private val origin: UploadCall,
    ) : UploadCall by origin {

        override fun clone(): UploadCall {
            return CachingCall(destination, origin.clone())
        }

        override fun execute(): UploadResponse {
            return origin.execute().cache()
        }

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
            if (body != null) synchronized(queue) {
                queue[destination] = body
            }
        }

    }

}
