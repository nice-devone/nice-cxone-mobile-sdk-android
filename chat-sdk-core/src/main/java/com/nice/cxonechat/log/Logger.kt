package com.nice.cxonechat.log

/**
 * Abstraction over logging implementation. The implementation is agnostic
 * but should be pluggable to almost any framework out there.
 * */
interface Logger {

    /**
     * Uses [level] to determine whether it should be passed to the underlying
     * implementation. Implementations are free to use whichever level they like.
     *
     * @param level Used to determine logging level. Platform levels are strongly
     * encouraged
     * @param message Message supplied to the implementation. Note that
     * implementations might have limits on message length.
     * @param throwable Error that occurred or null if the message only carries
     * information
     * */
    fun log(level: Level, message: String, throwable: Throwable? = null)

}
