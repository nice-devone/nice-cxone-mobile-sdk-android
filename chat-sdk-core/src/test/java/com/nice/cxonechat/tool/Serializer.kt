package com.nice.cxonechat.tool

import com.nice.cxonechat.socket.SocketDefaults

internal fun Any.serialize(): String = SocketDefaults.serializer.toJson(this)
