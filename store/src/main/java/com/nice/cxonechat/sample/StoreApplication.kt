/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.google.firebase.FirebaseApp
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Host application, initializes customized Emoji and Firebase.
 */
@HiltAndroidApp
class StoreApplication : Application() {
    @Inject
    internal lateinit var chatSettingsRepository: ChatSettingsRepository

    override fun onCreate() {
        super.onCreate()

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
    }
}
