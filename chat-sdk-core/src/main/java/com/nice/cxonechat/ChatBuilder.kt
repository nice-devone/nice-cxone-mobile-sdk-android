package com.nice.cxonechat

import android.content.Context
import androidx.annotation.CheckResult
import com.nice.cxonechat.internal.ChatBuilderDefault
import com.nice.cxonechat.internal.ChatBuilderLogging
import com.nice.cxonechat.internal.ChatBuilderRepeating
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.ChatEntrailsAndroid
import com.nice.cxonechat.internal.SocketFactory
import com.nice.cxonechat.internal.SocketFactoryDefault

@Public
interface ChatBuilder {

    /**
     * Sets authorization for newly created instance of the chat. It's used in the
     * case where Authorization is enabled in the backend configuration.
     *
     * Defaults to [Authorization.None].
     * */
    fun setAuthorization(authorization: Authorization): ChatBuilder

    /**
     * Sets a development mode. This can have various effects throughout the SDK
     * such as verbose logging.
     *
     * Defaults to `false`.
     * */
    fun setDevelopmentMode(enabled: Boolean): ChatBuilder

    /**
     * Sets a default username.
     * If the username changes invoke this method again and build the new chat to apply
     * the changes.
     * Usually, this should be done with app's lifecycle events automatically.
     * Name is updated with every eligible event, likely will be updated during
     * the authorization step when running [build].
     *
     * Defaults to empty values.
     * */
    fun setUserName(first: String, last: String): ChatBuilder

    /**
     * Builds an instance of chat asynchronously. It's guaranteed to retrieve an
     * instance of the chat. The method continuously polls the server when failure
     * occurs with exponential backoff where base equals to 2 seconds. All
     * failures are logged if [setDevelopmentMode] is set.
     *
     * If the instance is not retrieved within reasonable amount of time, the
     * device is not connected to the internet or the chat provider experiences
     * outage or your instance is misconfigured. In all of these cases consult
     * a representative.
     *
     * Can be called from any thread, but will change to non-main thread immediately.
     *
     * @see OnChatBuiltCallback.onChatBuilt
     */
    @CheckResult
    fun build(callback: OnChatBuiltCallback): Cancellable

    /**
     * Callback allowing to listen to chat instance provisioning.
     * @see build
     * */
    @Public
    fun interface OnChatBuiltCallback {
        /**
         * Notifies the consumer that a chat instance is ready. It's always called
         * on the main thread.
         * */
        fun onChatBuilt(chat: Chat)
    }

    @Public
    companion object {

        /**
         * Returns an instance of [ChatBuilder] with Android specific parameters.
         * @see build
         * @see OnChatBuiltCallback
         * @see OnChatBuiltCallback.onChatBuilt
         * */
        @JvmName("getDefault")
        operator fun invoke(
            context: Context,
            config: SocketFactoryConfiguration,
        ): ChatBuilder {
            val factory = SocketFactoryDefault(config)
            val entrails = ChatEntrailsAndroid(context.applicationContext, factory, config)
            return invoke(
                entrails = entrails,
                factory = factory
            )
        }

        internal operator fun invoke(
            entrails: ChatEntrails,
            factory: SocketFactory,
        ): ChatBuilder {
            var builder: ChatBuilder
            builder = ChatBuilderDefault(entrails, factory)
            builder = ChatBuilderLogging(builder, entrails)
            builder = ChatBuilderRepeating(builder, entrails)
            return builder
        }
    }
}
