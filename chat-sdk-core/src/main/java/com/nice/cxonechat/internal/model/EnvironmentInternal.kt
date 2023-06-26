package com.nice.cxonechat.internal.model

import com.nice.cxonechat.state.Environment

internal data class EnvironmentInternal(
    override val name: String,
    override val location: String,
    override val baseUrl: String,
    override val socketUrl: String,
    override val originHeader: String,
    override val chatUrl: String,
) : Environment() {

    override fun toString() = buildString {
        append("Environment(name='")
        append(name)
        append("', location='")
        append(location)
        append("', baseUrl='")
        append(baseUrl)
        append("', socketUrl='")
        append(socketUrl)
        append("', originHeader='")
        append(originHeader)
        append("', chatUrl='")
        append(chatUrl)
        append("')")
    }
}
