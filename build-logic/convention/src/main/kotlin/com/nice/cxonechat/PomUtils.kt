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

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom

private fun Project.prop(key: String): String? =
    providers.gradleProperty(key).getOrNull()

/**
 * Configures standard POM metadata for a Maven publication.
 * All values fall back to root project properties, then hardcoded defaults.
 * Property keys are defined in gradle.properties (POM_NAME, POM_DESCRIPTION, etc.).
 */
fun Project.configurePomMetadata(pom: MavenPom) {
    pom.name.set(prop("POM_NAME") ?: "CXone Chat")
    pom.description.set(prop("POM_DESCRIPTION") ?: "Android SDK implementation for CXone Chat")
    pom.inceptionYear.set(prop("POM_INCEPTION_YEAR") ?: "2022")
    pom.url.set(prop("POM_URL") ?: "https://github.com/nice-devone/nice-cxone-mobile-sdk-android/")

    pom.licenses {
        license {
            name.set(prop("POM_LICENSE_NAME") ?: "Proprietary")
            url.set(prop("POM_LICENSE_URL") ?: "https://www.nice.com/company/legal")
            distribution.set(prop("POM_LICENSE_DIST") ?: "repo")
        }
    }

    pom.developers {
        developer {
            id.set(prop("POM_DEVELOPER_ID") ?: "BrandEmbassy")
            name.set(prop("POM_DEVELOPER_NAME") ?: "NICE")
            url.set(prop("POM_DEVELOPER_URL") ?: "https://github.com/BrandEmbassy/")
        }
    }

    pom.scm {
        url.set(prop("POM_SCM_URL") ?: "https://github.com/nice-devone/nice-cxone-mobile-sdk-android/")
        connection.set(
            prop("POM_SCM_CONNECTION")
                ?: "scm:git:git://github.com/nice-devone/nice-cxone-mobile-sdk-android/.git"
        )
        developerConnection.set(
            prop("POM_SCM_DEV_CONNECTION")
                ?: "scm:git:ssh://git@github.com/nice-devone/nice-cxone-mobile-sdk-android/.git"
        )
    }
}

/**
 * Returns the current git branch name, or an empty string on failure.
 * Uses [providers.exec][org.gradle.api.provider.ProviderFactory.exec] so the result is
 * tracked by Gradle's configuration cache — the cache is invalidated when HEAD changes.
 */
internal fun Project.getGitCurrentBranch(): String =
    providers.exec {
        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
    }.standardOutput.asText.map { it.trim() }.orElse("").get()

/**
 * Combines [versionBase] with the current git branch name to produce an app version string.
 * Replaces `/` and `_` with `-` in the branch name.
 * Used for the store sample app's versionName.
 */
fun Project.branchVersionForApp(versionBase: String): String {
    val branch = getGitCurrentBranch()
    return "$versionBase-${branch.replace(Regex("[/_]"), "-")}"
}

/**
 * Combines [versionBase] with the current git branch name to produce a Maven snapshot version.
 * Only preserves the branch name if it starts with "feature/"; otherwise uses "main".
 * Appends "-SNAPSHOT" suffix. Used for local Maven publishing of dev builds.
 */
fun Project.branchVersionForMaven(versionBase: String): String {
    val featurePrefix = "feature/"
    val defaultBranch = "main"
    val branch = getGitCurrentBranch().let { branch ->
        if (branch.startsWith(featurePrefix)) branch else defaultBranch
    }
    return "$versionBase-${branch.replace(Regex("[/_]"), "-")}-SNAPSHOT"
}
