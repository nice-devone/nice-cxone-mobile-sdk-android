package com.nice.cxonechat.internal.socket

/**
 * Specification for WebSocket setup.
 */
internal object WebSocketSpec {
    /**
     * Status code as defined by
     *     [Section 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4).
     * 1000 indicates a normal closure, meaning that the purpose for which the connection
     * was established has been fulfilled.
     */
    const val CLOSE_NORMAL_CODE = 1000
}
