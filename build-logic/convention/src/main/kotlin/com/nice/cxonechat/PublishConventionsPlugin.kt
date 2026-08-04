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
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** Convention plugin that configures Maven publishing to GitHub Packages and a local branch-versioned publish task. */
abstract class PublishConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "maven-publish")
            val ghUsername = providers.gradleProperty("github.user")
                .orElse(providers.environmentVariable("GPR_USERNAME"))
                .getOrNull()
            val ghPassword = providers.gradleProperty("github.key")
                .orElse(providers.environmentVariable("GPR_TOKEN"))
                .getOrNull()
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "github"
                        url = uri("https://maven.pkg.github.com/nice-devone/nice-cxone-mobile-sdk-android")
                        credentials {
                            username = ghUsername
                            password = ghPassword
                        }
                    }
                }
            }
            // Computes branch version at configuration time using providers.exec so the result
            // is tracked by Gradle's configuration cache — cache is invalidated when HEAD changes.
            val baseVersion = providers.gradleProperty("VERSION_NAME").getOrNull() ?: version.toString()
            val branchVersion = branchVersionForMaven(baseVersion)
            // Publishes to MavenLocal with version + branch name.
            // Uses "main" for non-feature branches; appends "-SNAPSHOT" suffix.
            tasks.register("publishToMavenLocalAsBranchVersion") {
                group = "Publishing"
                description = "Publishes artifact to MavenLocal using current version + branch as artifact name"
                doFirst {
                    version = branchVersion
                    logger.lifecycle("Publishing local version: $branchVersion")
                }
                finalizedBy(tasks.named("publishToMavenLocal"))
            }
        }
    }
}
