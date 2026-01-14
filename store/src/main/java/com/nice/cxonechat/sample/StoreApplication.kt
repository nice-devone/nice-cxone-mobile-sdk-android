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

package com.nice.cxonechat.sample

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.google.firebase.FirebaseApp
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.modules.StoreModule
import com.nice.cxonechat.sample.utilities.FileLogger
import com.nice.cxonechat.sample.utilities.logging.FirebaseLogger
import com.nice.cxonechat.ui.UiModule.Companion.chatUiModule
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.utilities.TaggingSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.ksp.generated.module

/**
 * Host application, initializes customized Emoji and Firebase.
 */
class StoreApplication :
    Application(),
    SingletonImageLoader.Factory,
    LoggerScope by LoggerScope<StoreApplication>(sampleAppLogger) {

    private val okHttpClient by lazy {
        OkHttpClient
            .Builder()
            .socketFactory(TaggingSocketFactory)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = BASIC
                }
            )
            .build()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    override fun onCreate() = scope("onCreate") {
        super.onCreate()
        duration {
            startDi()
            improveEmulatorSupport()
            /* set up Firebase */
            FirebaseApp.initializeApp(applicationContext)
            enableStrictMode()
        }
    }

    private fun startDi() = scope("startDi") {
        duration {
            startKoin {
                androidContext(applicationContext)
                modules(
                    StoreModule().module,
                    module {
                        single { applicationScope }
                        single(named(NAME)) { sampleAppLogger as Logger }
                    }
                )
                includes(
                    chatUiModule(
                        logger = ProxyLogger(
                            FileLogger.getInstance(applicationContext),
                            FirebaseLogger(),
                            LoggerAndroid("CXoneChatUi"),
                        ),
                        customerFieldsProvider = get<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)),
                        contactFieldsProvider = get<UiCustomFieldsProvider>(named(CustomFieldProviderType.Contact)),
                    )
                )
            }
        }
    }

    private fun improveEmulatorSupport() = scope("improveEmulatorSupport") {
        duration {
            /*
             SampleApp is using a bundled version of an emoji support library,
             for better support of the latest emojis on clean emulator instances.
             Clean emulator instances require setup & time in order to properly
             load the latest fonts which support current emojis.

             For usage on real devices, the DefaultEmojiCompatConfig is a better option,
             since it will download the font which can be updated without the need to
             update the bundled artifact.
             */
            EmojiCompat.init(BundledEmojiCompatConfig(this@StoreApplication, Dispatchers.IO.asExecutor()))
        }
    }

    private fun enableStrictMode() = scope("enableStrictMode") {
        duration {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                StrictModePolicy.apply()
            } else {
                StrictMode.setThreadPolicy(
                    ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )

                StrictMode.setVmPolicy(
                    VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
            }
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = scope("newImageLoader") {
        duration {
            ImageLoader.Builder(context)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { okHttpClient }
                        )
                    )
                }
                .build()
        }
    }

    override fun onTerminate() = scope("onTerminate") {
        applicationScope.cancel("Application terminated")
        super.onTerminate()
    }

    internal companion object {
        /** Name & tag for the application logger. */
        internal const val NAME = "SampleApp"

        /** Shared logger for the application. */
        internal val sampleAppLogger: ProxyLogger = ProxyLogger(
            FirebaseLogger(),
            LoggerAndroid(NAME),
        )
    }
}
