/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.utilities

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * A javax.net.SocketFactory that delegates all creation requests to [origin] and then gives
 * the actual implementation a chance to configure the created socket.
 *
 * @property origin Base [SocketFactory] to which all creation requests are delegated.
 */
abstract class DelegatingSocketFactory(
    val origin: SocketFactory = getDefault(),
) : SocketFactory() {
    override fun createSocket(): Socket? = origin.createSocket().also(::configure)

    override fun createSocket(host: String?, port: Int): Socket? =
        origin.createSocket(host, port).also(::configure)

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket? =
        origin.createSocket(host, port, localHost, localPort).also(::configure)

    override fun createSocket(host: InetAddress?, port: Int): Socket? =
        origin.createSocket(host, port).also(::configure)

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket? =
        origin.createSocket(address, port, localAddress, localPort).also(::configure)

    /**
     * Configure a socket after it is created by [origin] and before it is returned
     * by `createSocket`.
     *
     * @param socket the socket to be configured.
     */
    abstract fun configure(socket: Socket)
}
