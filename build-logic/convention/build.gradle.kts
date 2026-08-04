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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Numeric (not lexicographic) comparison: "4.5.6" must compare as older than "4.5.13",
// which plain string comparison gets backwards once segment digit counts differ. A
// non-numeric segment falls back to string comparison against its counterpart, and a
// numeric segment always outranks a non-numeric one at the same position. Kept in sync
// with the equivalent function in root build.gradle.kts (duplicated because build-logic
// is a separate included build).
private fun isOlderThan(version: String?, minVersion: String): Boolean {
    fun segments(v: String) = v.replace('-', '.').split('.')
    val current = segments(version.orEmpty())
    val min = segments(minVersion)
    for (i in 0 until maxOf(current.size, min.size)) {
        val partCurrent = current.getOrElse(i) { "0" }
        val partMin = min.getOrElse(i) { "0" }
        val numCurrent = partCurrent.toIntOrNull()
        val numMin = partMin.toIntOrNull()
        val comparison = when {
            numCurrent != null && numMin != null -> numCurrent.compareTo(numMin)
            numCurrent != null -> 1
            numMin != null -> -1
            else -> partCurrent.compareTo(partMin)
        }
        if (comparison != 0) return comparison < 0
    }
    return false
}

plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint)
    alias(libs.plugins.detekt)
}

group = "com.nice.cxonechat.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// Plugin JARs are compileOnly: AGP, Kotlin plugin, etc. are already on the consuming
// project's runtime classpath. Bundling them via implementation would cause conflicts.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.dokka.gradlePlugin)
    compileOnly(libs.koin.gradlePlugin)
    lintChecks(libs.androidx.lint.gradle)
    detektPlugins(libs.detekt.formatting)
}

configurations.configureEach {
    resolutionStrategy {
        eachDependency {
            // AGP build tools pull in Bouncy Castle with a covert timing channel vulnerability
            // affecting FrodoEngine (CVE-2026-5598 / GHSA-p93r-85wp-75v3); fixed in 1.84.
            if (requested.group == "org.bouncycastle" && isOlderThan(requested.version, "1.84")) {
                useVersion("1.84")
            }
            // Update commons-lang3 to secure version
            if (requested.group == "org.apache.commons" && requested.name == "commons-lang3" && isOlderThan(requested.version, "3.18.0")) {
                useVersion("3.18.0")
            }
            // Update httpclient to secure version
            if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" && isOlderThan(requested.version, "4.5.13")) {
                useVersion("4.5.13")
            }
            // AGP lint tooling pulls in older Jackson (GHSA-5jmj-h7xm-6q6v); fixed in 2.18.9.
            // Mirrors the root build's fix — this is a separate included build and does not
            // inherit resolutionStrategy from the root project.
            if (requested.group.startsWith("com.fasterxml.jackson") && isOlderThan(requested.version, "2.18.9")) {
                useVersion("2.18.9")
            }
            // AGP bundletool is using a vulnerable jose4j version (CVE-2024-29371); fixed in 0.9.6.
            // Mirrors the root build's fix — see the Jackson rule above for why this is needed here too.
            if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" && isOlderThan(requested.version, "0.9.6")) {
                useVersion("0.9.6")
            }
            // AGP jetifier-processor is using a vulnerable jdom2 version (CVE-2021-33813); fixed in 2.0.6.1.
            // Mirrors the root build's fix — see the Jackson rule above for why this is needed here too.
            if (requested.group == "org.jdom" && requested.name == "jdom2" && isOlderThan(requested.version, "2.0.6.1")) {
                useVersion("2.0.6.1")
            }
            if (requested.group.startsWith("com.fasterxml.jackson") && (requested.version ?: "") < "2.18.9") {
                useVersion("2.18.9")
            }
        }
    }
}

detekt {
    config.setFrom(
        rootDir.resolve("../config/detekt/detekt-common.yml"),
        file("detekt-override.yml"),
    )
    buildUponDefaultConfig = true
}

lint {
    baseline = file("lint-baseline.xml")
}

dependencyLocking {
    lockAllConfigurations()
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplicationConventions") {
            id = libs.plugins.cxone.chat.android.application.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidApplicationConventionsPlugin"
        }
        register("androidDocsConventions") {
            id = libs.plugins.cxone.chat.android.docs.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidDocsConventionsPlugin"
        }
        register("androidKotlinConventions") {
            id = libs.plugins.cxone.chat.android.kotlin.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidKotlinConventionsPlugin"
        }
        register("androidLibraryConventions") {
            id = libs.plugins.cxone.chat.android.library.asProvider().get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidLibraryConventionsPlugin"
        }
        register("androidLibraryStyleConventions") {
            id = libs.plugins.cxone.chat.android.library.style.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidLibraryStyleConventionsPlugin"
        }
        register("androidTestConventions") {
            id = libs.plugins.cxone.chat.android.test.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidTestConventionsPlugin"
        }
        register("androidUiConventions") {
            id = libs.plugins.cxone.chat.android.ui.get().pluginId
            implementationClass = "com.nice.cxonechat.AndroidUiConventionsPlugin"
        }
        register("applicationStyleConventions") {
            id = libs.plugins.cxone.chat.application.style.get().pluginId
            implementationClass = "com.nice.cxonechat.ApplicationStyleConventionsPlugin"
        }
        register("docsConventions") {
            id = libs.plugins.cxone.chat.docs.get().pluginId
            implementationClass = "com.nice.cxonechat.DocsConventionsPlugin"
        }
        register("javaLibraryConventions") {
            id = libs.plugins.cxone.chat.java.library.get().pluginId
            implementationClass = "com.nice.cxonechat.JavaLibraryConventionsPlugin"
        }
        register("jvmKotlinConventions") {
            id = libs.plugins.cxone.chat.jvm.kotlin.get().pluginId
            implementationClass = "com.nice.cxonechat.JvmKotlinConventionsPlugin"
        }
        register("koinConventions") {
            id = libs.plugins.cxone.chat.koin.get().pluginId
            implementationClass = "com.nice.cxonechat.KoinConventionsPlugin"
        }
        register("kotlinConventions") {
            id = libs.plugins.cxone.chat.kotlin.get().pluginId
            implementationClass = "com.nice.cxonechat.KotlinConventionsPlugin"
        }
        register("libraryStyleConventions") {
            id = libs.plugins.cxone.chat.library.style.get().pluginId
            implementationClass = "com.nice.cxonechat.LibraryStyleConventionsPlugin"
        }
        register("publishConventions") {
            id = libs.plugins.cxone.chat.publish.get().pluginId
            implementationClass = "com.nice.cxonechat.PublishConventionsPlugin"
        }
        register("testConventions") {
            id = libs.plugins.cxone.chat.test.get().pluginId
            implementationClass = "com.nice.cxonechat.TestConventionsPlugin"
        }
        register("uiComposeConventions") {
            id = libs.plugins.cxone.chat.ui.compose.get().pluginId
            implementationClass = "com.nice.cxonechat.UiComposeConventionsPlugin"
        }
    }
}
