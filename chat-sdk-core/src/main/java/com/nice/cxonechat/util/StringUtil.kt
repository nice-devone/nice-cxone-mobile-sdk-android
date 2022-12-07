package com.nice.cxonechat.util

import kotlin.text.Typography.ellipsis

internal fun String.ellipsize(maxChars: Int = 60): String {
    return if (this.length <= maxChars) {
        this
    } else {
        this.substring(0 until maxChars) + ellipsis
    }
}
