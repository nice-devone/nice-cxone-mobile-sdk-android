package com.nice.cxonechat.message

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.ContentDescriptorInternal

/**
 * Container class for upload of attachment to backend.
 * This class holds both the blob and it's metadata.
 */
@Public
abstract class ContentDescriptor {
    /**
     * Attachment content which should be encoded in Base64 to
     * avoid Json collisions.
     * */
    abstract val content: String

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

    @Public
    companion object {

        /**
         * Constructs new [ContentDescriptor]
         * @see ContentDescriptor
         * */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(
            content: String,
            mimeType: String?,
            fileName: String?,
        ): ContentDescriptor = ContentDescriptorInternal(
            content = content,
            mimeType = mimeType,
            fileName = fileName
        )

    }
}
