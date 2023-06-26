package com.nice.cxonechat

/**
 * Listener for [Chat] instance state changes.
 * The current main purpose of this listener is to provide callbacks which notify integrating application about
 * chat session connectivity changes.
 */
@Public
interface ChatStateListener {
    /**
     * Method is invoked when chat session is unexpectedly lost.
     * [Chat] instance should be considered unusable, until reconnect is performed.
     * If this method is invoked while the [Chat.reconnect] is being performed, then application should consider the
     * reconnection attempt as failed. It is recommended that only a limited number of reconnection attempts are
     * performed.
     * If the problems persist despite the fact that the device has internet connectivity,
     * then please collect logs (assuming the development mode is enabled) and contact your CXone representative
     * with request for support.
     * Integration should also check for possibly lost incoming messages & thread changes once reconnection is done.
     */
    fun onUnexpectedDisconnect()

    /**
     * Method is invoked when chat instance is connected and ready to send/receive messages, events or actions.
     * This happens once initial connection is established or after [Chat.reconnect] is called.
     */
    fun onConnected()
}
