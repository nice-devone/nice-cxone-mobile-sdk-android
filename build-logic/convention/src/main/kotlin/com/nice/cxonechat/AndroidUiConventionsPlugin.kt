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
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention plugin for Android UI modules. Enables vector drawable support,
 * strict Java interop checks, and Compose navigation dependencies.
 */
abstract class AndroidUiConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.nice.cxone.ui.compose")
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    defaultConfig { vectorDrawables.useSupportLibrary = true }
                }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    defaultConfig { vectorDrawables.useSupportLibrary = true }
                }
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-Xjspecify-annotations=strict",
                        )
                    )
                }
            }
            dependencies {
                "implementation"(libs.findLibrary("androidx.appcompat").get())
                "implementation"(libs.findLibrary("androidx.ktx").get())
                "implementation"(libs.findLibrary("androidx.datastore").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel").get())
                "implementation"(libs.findLibrary("kotlinx.coroutines").get())
                "implementation"(libs.findLibrary("androidx.navigation.ui").get())
                "implementation"(libs.findLibrary("androidx.navigation.runtime").get())
                "implementation"(libs.findLibrary("androidx.navigation.compose").get())
            }
        }
    }
}
