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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.gradle.DokkaExtension

/** Convention plugin for Android documentation generation via Dokka. Enables Android doc links and suppresses debug sources. */
abstract class AndroidDocsConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.nice.cxone.docs")
            extensions.configure<DokkaExtension> {
                dokkaSourceSets.configureEach {
                    // Enable Android doc links for release source sets only.
                    // Using configureEach (not named("test")) so this works for both
                    // Android module flavors (release/debug) and JVM modules (non-test).
                    // See https://github.com/Kotlin/dokka/issues/4472 for more information
                    // Investigate if the workaround can be removed as part of DE-166077
                    suppress.set(true)
                    if (name.contains("release", ignoreCase = true) && !name.contains("test", ignoreCase = true)) {
                        suppress.set(false)
                        enableAndroidDocumentationLink.set(true)
                    }
                }
            }
            dependencies {
                "dokkaPlugin"(libs.findLibrary("dokka.android").get())
            }
        }
    }
}
