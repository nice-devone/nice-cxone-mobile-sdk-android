package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.serializer.Default

internal fun Any.serialize(): String = Default.serializer.toJson(this)
