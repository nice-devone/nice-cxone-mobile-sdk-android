package com.nice.cxonechat

import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.state.Environment

/**
 * Configuration for the instance that will be invoked by the [ChatBuilder].
 */
@Public
interface SocketFactoryConfiguration {

    /**
     * Current environment to connect to.
     * Consult a representative to discover which is right for you.
     *
     * @see CXOneEnvironment
     */
    val environment: Environment

    /**
     * Brand id to connect under.
     * Consult a representative to discover your given brand id.
     *
     * It Should be a 4-digit number, but it can be updated in the future.
     */
    val brandId: Long

    /**
     * Channel id to connect under. Consult a representative to discover
     * your given channel id.
     */
    val channelId: String

    /**
     * Client version to be reported.
     * The library always supplies its own version, though you might be asked
     * (or willing to try) to change it in case of beta features, for example.
     */
    val version: String

    @Public
    companion object {

        /**
         * Helper method to create a new configuration.
         *
         * @see SocketFactoryConfiguration
         */
        @JvmName("create")
        @JvmStatic
        @JvmOverloads
        operator fun invoke(
            environment: Environment,
            brandId: Long,
            channelId: String,
            version: String = BuildConfig.VERSION_NAME,
        ) = object : SocketFactoryConfiguration {
            override val environment = environment
            override val brandId = brandId
            override val channelId = channelId
            override val version = version
        }
    }
}
