package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

// ChannelView

/**
 * Uniquely identifies a channel.
 */
internal data class ChannelIdentifier(

    /** The id of the channel. */
    @SerializedName("id")
    val id: String,
)
