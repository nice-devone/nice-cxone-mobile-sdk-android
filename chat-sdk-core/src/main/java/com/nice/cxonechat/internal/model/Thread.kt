package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

// ThreadView

/** Represents info about a thread from the socket. */
internal data class Thread(
    /** The unique id for the thread. */
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,

    /** The name given to the thread (for multi-chat channels only). */
    @SerializedName("threadName")
    val threadName: String? = null,
) {

    constructor(
        thread: ChatThread,
    ) : this(
        idOnExternalPlatform = thread.id,
        threadName = thread.threadName
    )
}
