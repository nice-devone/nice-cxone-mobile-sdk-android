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
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register

/**
 * Convention plugin that applies the Dokka documentation plugin.
 *
 * When a module also applies the `org.jetbrains.dokka-javadoc` plugin, registers a shared `javadocJar` task
 * packaging Dokka's own Javadoc-format output. Publishable modules attach this task's output to their Maven
 * publication manually instead of using AGP's `withJavadocJar()`, which is unusable on JVM 17 due to an
 * unfixed upstream bug in AGP's bundled Dokka worker (KT-60197, "PermittedSubclasses requires ASM9").
 */
abstract class DocsConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.dokka")
            pluginManager.withPlugin("org.jetbrains.dokka-javadoc") {
                tasks.register<Jar>("javadocJar") {
                    archiveClassifier.set("javadoc")
                    from(tasks.named("dokkaGeneratePublicationJavadoc"))
                }
            }
        }
    }
}
