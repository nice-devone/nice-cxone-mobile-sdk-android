/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.sample.modules.StoreModule
import com.nice.cxonechat.sample.utilities.logging.FirebaseLogger
import com.nice.cxonechat.ui.UiModule.Companion.chatUiModule
import com.nice.cxonechat.utilities.TaggingSocketFactory
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

/**
 * Host application, initializes customized Emoji and Firebase.
 */
class StoreApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            chatUiModule(
                ProxyLogger(
                    FirebaseLogger(),
                    LoggerAndroid("CXoneChatUi")
                )
            )
            modules(StoreModule().module)
        }

        /*
         SampleApp is using a bundled version of an emoji support library,
         for better support of the latest emojis on clean emulator instances.
         Clean emulator instances require setup & time in order to properly
         load the latest fonts which support current emojis.

         For usage on real devices, the DefaultEmojiCompatConfig is a better option,
         since it will download the font which can be updated without the need to
         update the bundled artifact.
         */
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        /* set up Firebase */
        FirebaseApp.initializeApp(applicationContext)

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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(
                OkHttpClient.Builder()
                    .socketFactory(TaggingSocketFactory)
                    .build()
            )
            .build()
    }
}
