@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat.api

import com.nice.cxonechat.internal.RemoteServiceBuilder
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.state.Connection
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Protocol.HTTP_2
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.net.ConnectException

internal class RemoteServiceTest {

    private lateinit var builder: RemoteServiceBuilder
    private lateinit var connection: Connection

    @Before
    fun prepare() {
        connection = makeConnection()
        builder = RemoteServiceBuilder()
            .setConnection(connection)
    }

    @Test
    fun upload_cachesIdenticalAttachments() {
        val interceptor = spy(UploadInterceptor())
        val client = builder
            .setInterceptor(interceptor)
            .build()
        val upload = AttachmentUploadModel("content", "mime", "name")
        client.uploadFile(upload, "0", "channelId").execute()
        client.uploadFile(upload, "0", "channelId").execute()
        verify(interceptor, times(1)).intercept(any())
    }

    open class UploadInterceptor : Interceptor {

        override fun intercept(chain: Chain): Response {
            val request = chain.request().newBuilder()
                .url("http://localhost/foo")
                .build()
            @Suppress("SwallowedException")
            return try {
                chain.proceed(request)
            } catch (e: ConnectException) {
                Response.Builder()
                    .request(request)
                    .protocol(HTTP_2)
                    .code(200)
                    .message("")
                    .body("""{"fileUrl":"fileUrl"}""".toResponseBody())
                    .build()
            }
        }
    }
}
