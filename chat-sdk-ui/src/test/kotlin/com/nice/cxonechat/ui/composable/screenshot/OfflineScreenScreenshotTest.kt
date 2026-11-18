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
 * AN "AS IS" BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.ui.composable.screenshot

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.screen.OfflineScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot coverage for [OfflineScreen] in portrait and landscape.
 *
 * The landscape qualifier matches the Figma reference frame (852x412, node 63133-47909), where the
 * illustration does not fit and the header bar is capped instead of spanning the whole window.
 *
 * Robolectric does not dispatch real system-bar insets to the decor view, so the status bar/cutout color
 * strip (`OfflineScreen`'s `windowInsetsTopHeight(WindowInsets.statusBars.union(WindowInsets.displayCutout))`
 * box) resolves to 0dp height in these goldens. That fix is out of scope for this test class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfflineScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // AsyncImage decodes R.drawable.offline asynchronously, and the tablet qualifier's larger target size makes
    // that decode slow enough to lose the race against captureRoboImage() on the core-bound CI runner (see the
    // unit-test job comment in test.yml), leaving the illustration blank only in CI. Stubbing the loader with the
    // real drawable - not a placeholder - returns the same pixels synchronously so the golden stays accurate.
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = requireNotNull(ContextCompat.getDrawable(context, R.drawable.offline))
        val engine = FakeImageLoaderEngine(drawable)
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context)
                .components { add(engine) }
                .build()
        )
    }

    // The loader is process wide and this module reuses one JVM across test classes, so leaving the stub
    // installed would carry the stubbed drawable into any later test that loads an image.
    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    private fun captureOfflineScreen() {
        composeTestRule.setContent {
            ChatTheme {
                OfflineScreen(onBackPress = {}, snackbarHostState = SnackbarHostState())
            }
        }
        // Settles the illustration's AnimatedVisibility enter transition, which can otherwise still be
        // fading in when the runner is core-bound - see the unit-test job comment in test.yml.
        composeTestRule.mainClock.advanceTimeBy(20_000L)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `offline screen portrait light`() {
        captureOfflineScreen()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `offline screen portrait dark`() {
        captureOfflineScreen()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-long-notround-notnight-xhdpi-finger")
    fun `offline screen landscape light`() {
        captureOfflineScreen()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-long-notround-night-xhdpi-finger")
    fun `offline screen landscape dark`() {
        captureOfflineScreen()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xlarge-notlong-notround-notnight-xhdpi-finger")
    fun `offline screen tablet landscape light`() {
        captureOfflineScreen()
    }
}
