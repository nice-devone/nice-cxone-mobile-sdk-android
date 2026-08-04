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

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Convention plugin for SDK library modules. Applies Detekt with library and formatting rules. */
abstract class LibraryStyleConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "io.gitlab.arturbosch.detekt")
            val detektRootDir = isolated.rootProject.projectDirectory
            extensions.configure<DetektExtension> {
                config.setFrom(
                    detektRootDir.file("config/detekt/detekt-common.yml"),
                    detektRootDir.file("config/detekt/detekt-library.yml"),
                )
                baseline = file("config/detekt/detekt-baseline.xml")
                ignoredBuildTypes = listOf("release")
                source.from(
                    fileTree("src/androidTest") { include("**/*.kt") },
                    fileTree("src/test") { include("**/*.kt") }
                )
            }
            dependencies {
                "detektPlugins"(libs.findLibrary("detekt.formatting").get())
                "detektPlugins"(libs.findLibrary("detekt.rules.libraries").get())
                "detektPlugins"(project(mapOf("path" to ":cxone-detekt-rules")))
            }
        }
    }
}
