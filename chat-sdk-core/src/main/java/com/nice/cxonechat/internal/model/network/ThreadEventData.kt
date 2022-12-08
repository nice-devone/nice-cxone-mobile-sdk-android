package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.Thread

/**
 * Event data to be sent for any thread event (archive, recover, etc.).
 */
internal data class ThreadEventData(
    @SerializedName("thread")
    val thread: Thread,
)
