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
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Convention plugin for Android application modules. Applies Detekt with application rules, lint baseline, and error-on-warning. */
abstract class ApplicationStyleConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Idempotent — AGP already applied by android-application-conventions
            apply(plugin = "com.android.application")
            apply(plugin = "io.gitlab.arturbosch.detekt")
            val detektRootDir = isolated.rootProject.projectDirectory
            extensions.configure<DetektExtension> {
                config.setFrom(
                    detektRootDir.file("config/detekt/detekt-common.yml"),
                    detektRootDir.file("config/detekt/detekt-application.yml"),
                )
                baseline = file("config/detekt/detekt-baseline.xml")
                ignoredBuildTypes = listOf("release")
            }
            dependencies {
                "detektPlugins"(libs.findLibrary("detekt.formatting").get())
                "detektPlugins"(project(mapOf("path" to ":cxone-detekt-rules")))
            }
            extensions.configure<ApplicationExtension> {
                lint {
                    baseline = file("lint-baseline.xml")
                    abortOnError = true
                    checkAllWarnings = true
                }
            }
            tasks.withType<Detekt>().configureEach {
                source(fileTree("src/androidTest") { include("**/*.kt") })
                source(fileTree("src/test") { include("**/*.kt") })
            }
        }
    }
}
