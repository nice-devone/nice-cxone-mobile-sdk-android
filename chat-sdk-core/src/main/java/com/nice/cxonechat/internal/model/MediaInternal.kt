package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MediaModel
import com.nice.cxonechat.message.Media

internal data class MediaInternal(
    override val fileName: String,
    override val url: String,
    override val mimeType: String
) : Media {
    constructor(model: MediaModel)
    : this(model.fileName, model.url, model.mimeType)
}
