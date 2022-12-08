package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class EventThreadListFetched(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val threads get() = postback.data.threads

    data class Data(
        @SerializedName("threads")
        val threads: List<ReceivedThreadData>,
    )

}
