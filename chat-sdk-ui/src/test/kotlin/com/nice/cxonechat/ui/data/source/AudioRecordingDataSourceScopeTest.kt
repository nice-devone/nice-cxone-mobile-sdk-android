/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.data.source

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.screen.ChatActivity
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.TypeQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.includes
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the activity-scope `Context` shadowing: inside [ChatActivity]'s Koin scope an unqualified `Context` resolves to
 * the scope source, the Activity, and a root factory resolves its dependencies in the requesting scope.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class AudioRecordingDataSourceScopeTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private lateinit var activityScope: Scope

    @Before
    fun setUp() {
        val koin = startKoin {
            androidContext(application)
            includes(UiModule.chatUiModule())
        }.koin
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        activityScope = koin.createScope(
            scopeId = "AudioRecordingDataSourceScopeTest",
            qualifier = TypeQualifier(ChatActivity::class),
            source = activity,
        )
    }

    @After
    fun tearDown() {
        activityScope.close()
        stopKoin()
    }

    @Test
    fun `data source resolved from the activity scope holds the application context`() {
        val dataSource = activityScope.get<AudioRecordingDataSource>()

        assertSame(application, dataSource.context)
    }

    @Test
    fun `release without a recording session is a no-op`() {
        val dataSource = activityScope.get<AudioRecordingDataSource>()

        dataSource.release()
        dataSource.release()
    }
}
