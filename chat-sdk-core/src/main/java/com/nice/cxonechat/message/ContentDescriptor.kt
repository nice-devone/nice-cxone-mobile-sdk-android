package com.nice.cxonechat.message

import android.content.Context
import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.ContentDescriptorInternal

/**
 * Container class for upload of attachment to backend.
 * This class holds both the blob and its metadata.
 */
@Public
abstract class ContentDescriptor {
    /**
     * Describes where the data in a content descriptor originates.
     *
     * This exists to allow content to be shared from a large file or
     * other asset with minimal memory impact.
     */
    @Public
    sealed class DataSource {
        /**
         * Data originates as a ByteArray.
         *
         * @property bytes actual content
         */
        class Bytes internal constructor(val bytes: ByteArray): DataSource() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Bytes

                if (!bytes.contentEquals(other.bytes)) return false

                return true
            }

            override fun hashCode(): Int = bytes.contentHashCode()

            override fun toString(): String = "DataSource(length=${bytes.size})"
        }

        /**
         * Data originates as a, possibly Context-specific, URI.
         *
         * @property uri original uri of content
         * @property context Android context to be used to open the [uri]
         */
        class Uri internal constructor(
            val uri: android.net.Uri,
            val context: Context
        ): DataSource() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Uri

                if (uri != other.uri) return false

                return true
            }

            override fun hashCode(): Int = uri.hashCode()

            override fun toString(): String = "DataSource(uri=$uri)"
        }
    }

    /**
     * Attachment content.
     */
    abstract val content: DataSource

    /**
     * Mime type of provided [content].
     * It's required to properly deserialize the file after upload.
     *
     * Visit [IANA](https://www.iana.org/) for valid mime types
     * */
    abstract val mimeType: String?

    /**
     * Name of provided in [content].
     * Should contain the file name extension corresponding to the [mimeType].
     * */
    abstract val fileName: String?

    /**
     * Friendly name provided in [content].
     *
     * Typically, the original file name of the content to be uploaded
     */
    abstract val friendlyName: String?

    @Public
    companion object {
        /**
         * Constructs new [ContentDescriptor] with [android.net.Uri] data.
         * @see ContentDescriptor
         * @param content Uri for content to be attached, usually a content URI
         * @param context [Context] used to resolve [content]
         * @param mimeType MIME type of data
         * @param fileName obscured name of file
         * @param friendlyName friendly name of file
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(
            content: android.net.Uri,
            context: Context,
            mimeType: String?,
            fileName: String?,
            friendlyName: String?
        ): ContentDescriptor = ContentDescriptorInternal(
            content = DataSource.Uri(content, context),
            mimeType = mimeType,
            fileName = fileName,
            friendlyName = friendlyName
        )

        /**
         * Constructs new [ContentDescriptor] with [ByteArray] data.
         * @see ContentDescriptor
         * @param content [DataSource] described
         * @param mimeType MIME type of data
         * @param fileName obscured name of file
         * @param friendlyName friendly name of file
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(
            content: ByteArray,
            mimeType: String?,
            fileName: String?,
            friendlyName: String?
        ): ContentDescriptor = ContentDescriptorInternal(
            content = DataSource.Bytes(content),
            mimeType = mimeType,
            fileName = fileName,
            friendlyName = friendlyName
        )
    }
}
