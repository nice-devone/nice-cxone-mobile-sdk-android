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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** Convention plugin for Android library lint style checks. Configures HTML reports and enables Java interop checks. */
abstract class AndroidLibraryStyleConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply AGP and Kotlin first (idempotent — already applied by android-library-conventions)
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "com.nice.cxone.library.style")
            extensions.configure<LibraryExtension> {
                lint {
                    htmlReport = true
                    enable += "CheckResult"
                    enable += setOf("KotlinPropertyAccess", "LambdaLast", "NoHardKeywords")
                    baseline = file("lint-baseline.xml")
                }
            }
        }
    }
}
