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

package com.nice.cxonechat

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Convention plugin that wires up the Android instrumentation test runner and adds standard test dependencies. */
abstract class AndroidTestConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.nice.cxone.test")

            // Use withPlugin callbacks to configure the android extension safely
            // without requiring AGP in this plugin's own apply-time classpath.
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    defaultConfig {
                        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }
                }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    defaultConfig {
                        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }
                }
            }

            dependencies {
                "androidTestImplementation"(libs.findLibrary("androidx.test.runner").get())
                "androidTestImplementation"(libs.findLibrary("androidx.test.espresso").get())
                "androidTestImplementation"(libs.findLibrary("androidx.test.espresso.intents").get())
            }
        }
    }
}
