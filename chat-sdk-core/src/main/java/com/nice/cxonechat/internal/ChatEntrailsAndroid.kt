package com.nice.cxonechat.internal

import android.content.Context
import android.os.Looper
import androidx.core.os.HandlerCompat
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.PreferencesValueStorage
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.OkHttpClient
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

internal class ChatEntrailsAndroid(
    context: Context,
    factory: SocketFactory,
    config: SocketFactoryConfiguration,
    sharedClient: OkHttpClient,
) : ChatEntrails {

    override val storage: ValueStorage = PreferencesValueStorage(context)
    override val service: RemoteService = RemoteServiceBuilder()
        .setSharedOkHttpClient(sharedClient)
        .setConnection(factory.getConfiguration(storage))
        .build()
    override val threading: Threading = Threading(AndroidExecutor())
    override val environment: Environment = config.environment
    override val logger: Logger = LoggerAndroid()

    private class AndroidExecutor : AbstractExecutorService() {

        private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

        override fun execute(command: Runnable?) {
            handler.post(command ?: return)
        }

        override fun shutdown() = Unit
        override fun shutdownNow() = emptyList<Runnable>()
        override fun isShutdown() = false
        override fun isTerminated() = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit?) = false
    }
}
