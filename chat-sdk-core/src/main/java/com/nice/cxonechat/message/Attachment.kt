package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/** Represents info about an uploaded attachment. */
@Public
abstract class Attachment {
    /** The URL where the attachment can be found. */
    abstract val url: String

    /** A friendly name to display to the user. */
    abstract val friendlyName: String

    /** The mimeType for attachment. */
    abstract val mimeType: String?
}
