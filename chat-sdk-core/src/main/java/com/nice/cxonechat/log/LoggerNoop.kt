package com.nice.cxonechat.log

internal object LoggerNoop : Logger {

    override fun log(level: Level, message: String, throwable: Throwable?) {
        /* no-op */
    }
}
