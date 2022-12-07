package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class UTM constructor(
    @SerializedName("source")
    val source: String,
    @SerializedName("medium")
    val medium: String,
    @SerializedName("campaign")
    val campaign: String,
    @SerializedName("term")
    val term: String,
    @SerializedName("content")
    val content: String,
)
