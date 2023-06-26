package com.nice.cxonechat.internal.model

import android.util.Base64
import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.ContentDescriptor.DataSource
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

internal data class AttachmentUploadModel(
    @SerializedName("content")
    val content: String? = null,

    @SerializedName("mimeType")
    val mimeType: String? = null,

    @SerializedName("fileName")
    val fileName: String? = null,
) {
    /**
     * Convenience constructor that gets all necessary fields from the passed
     * [ContentDescriptor].
     *
     * @param upload [ContentDescriptor] containing attachment details
     * @throws IOException if the attachment cannot be read
     */
    @Throws(IOException::class)
    constructor(upload: ContentDescriptor): this(
        content = upload.content.read().base64,
        mimeType = upload.mimeType,
        fileName = upload.friendlyName
    )

    companion object {
        /**
         * Encode the receiving [ByteArray] as a base-64 encoded string.
         *
         * @receiver [ByteArray] to be encoded
         * @return receiver encoded as a base-64 encoded string
         */
        private val ByteArray.base64: String
            get() = Base64.encodeToString(this, Base64.DEFAULT)

        /**
         * Read the receiving [DataSource] as a ByteArray.
         *
         * @receiver [DataSource] to be encoded
         * @return data from [DataSource] as a base-64 encoded string
         * @throws IOException if the data source can't be read
         */
        @Throws(IOException::class)
        private fun DataSource.read() = when (this) {
            is DataSource.Uri ->
                context.contentResolver
                    .openInputStream(uri)
                    ?.use(InputStream::readBytes)
                    ?: throw FileNotFoundException(uri.toString())
            is DataSource.Bytes -> bytes
        }
    }
}
