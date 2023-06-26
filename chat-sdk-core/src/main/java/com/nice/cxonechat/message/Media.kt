package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Describe a piece of media/image to be displayed with an action or message.
 */
@Public
interface Media {
    /** name of media. */
    val fileName: String

    /** url from which to fetch media. */
    val url: String

    /** mime type of media. */
    val mimeType: String
}
