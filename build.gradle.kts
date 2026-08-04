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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

// Numeric (not lexicographic) comparison: e.g. "4.1.68.Final" must compare as older than
// "4.1.135.Final", which plain string comparison gets backwards once segment digit counts
// differ ("68" > "135" lexicographically, but 68 < 135 numerically). A non-numeric segment
// (e.g. netty's "Final"/"CR1" qualifier) falls back to string comparison against its
// counterpart so a pre-release still ranks below a final release at the same numeric prefix,
// and a numeric segment always outranks a non-numeric one at the same position.
fun compareVersions(a: String, b: String): Int {
    fun segments(v: String) = v.replace('-', '.').split('.')
    val segA = segments(a)
    val segB = segments(b)
    for (i in 0 until maxOf(segA.size, segB.size)) {
        val partA = segA.getOrElse(i) { "0" }
        val partB = segB.getOrElse(i) { "0" }
        val numA = partA.toIntOrNull()
        val numB = partB.toIntOrNull()
        val comparison = when {
            numA != null && numB != null -> numA.compareTo(numB)
            numA != null -> 1
            numB != null -> -1
            else -> partA.compareTo(partB)
        }
        if (comparison != 0) return comparison
    }
    return 0
}

fun isOlderThan(version: String?, minVersion: String) = compareVersions(version.orEmpty(), minVersion) < 0

buildscript {
    // The buildscript {} block is compiled and evaluated before the rest of this script, so it
    // cannot see the compareVersions/isOlderThan functions declared above — this is a local copy
    // scoped to this block only. Keep both copies in sync; see the matching function in
    // build-logic/convention/build.gradle.kts for the analogous constraint on that build.
    fun compareVersions(a: String, b: String): Int {
        fun segments(v: String) = v.replace('-', '.').split('.')
        val segA = segments(a)
        val segB = segments(b)
        for (i in 0 until maxOf(segA.size, segB.size)) {
            val partA = segA.getOrElse(i) { "0" }
            val partB = segB.getOrElse(i) { "0" }
            val numA = partA.toIntOrNull()
            val numB = partB.toIntOrNull()
            val comparison = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> 1
                numB != null -> -1
                else -> partA.compareTo(partB)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun isOlderThan(version: String?, minVersion: String) = compareVersions(version.orEmpty(), minVersion) < 0

    configurations.named("classpath") {
        resolutionStrategy.activateDependencyLocking()
    }
    dependencies {
        // Roborazzi's gradle plugin bundles roborazzi-core-jvm which has junit as a runtime
        // dependency. Gradle 9's InstrumentationClasspathMerger resolves it even though the
        // plugin POM excludes it, so we declare it explicitly to keep the lock file consistent.
        classpath(libs.junit)
    }
    configurations.configureEach {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "io.netty" && isOlderThan(requested.version, "4.1.135.Final")) {
                    useVersion("4.1.135.Final")
                }
                if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
                    val v = requested.version.orEmpty()
                    if (compareVersions(v, "1.21") >= 0 && compareVersions(v, "1.26.0") < 0) {
                        useVersion("1.26.0")
                    }
                }
                // AGP bundletool is using a vulnerable jose4j version (CVE-2024-29371)
                if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" && isOlderThan(requested.version, "0.9.6")) {
                    useVersion("0.9.6")
                }
                // AGP jetifier-processor is using a vulnerable jdom2 version (CVE-2021-33813)
                if (requested.group == "org.jdom" && requested.name == "jdom2") {
                    useVersion("2.0.6.1")
                }
                // AGP sdklib is using a vulnerable commons-lang3 version (CVE-2025-48924)
                if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
                    useVersion("3.18.0")
                }
                // AGP build tools pull in Bouncy Castle with a covert timing channel vulnerability
                // affecting FrodoEngine (CVE-2026-5598 / GHSA-p93r-85wp-75v3); fixed in 1.84.
                if (requested.group == "org.bouncycastle" && isOlderThan(requested.version, "1.84")) {
                    useVersion("1.84")
                }
                // GHSA-5jmj-h7xm-6q6v
                if (requested.group.startsWith("com.fasterxml.jackson") && isOlderThan(requested.version, "2.18.9")) {
                    useVersion("2.18.9")
                }
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka) apply true
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.androidx.safeargs) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.rootcoverage) apply true
    alias(libs.plugins.semantic.version) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.build.config) apply false
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.roborazzi) apply false
}

dependencyLocking {
    lockAllConfigurations()
}

subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy {
            eachDependency {
                // Android tests are using vulnerable dependency
                if (requested.group == "com.google.protobuf" && isOlderThan(requested.version, "3.25.5")) {
                    useVersion("3.25.5")
                }
                // Android tests are using vulnerable dependency
                if (requested.group == "io.grpc" && isOlderThan(requested.version, "1.75.0")) {
                    useVersion("1.75.0")
                }
                // Android tests are using vulnerable dependency
                if (requested.group == "io.netty" && isOlderThan(requested.version, "4.1.135.Final")) {
                    useVersion("4.1.135.Final")
                }
                // Android gradle plugin & root coverage plugin are using vulnerable dependency
                if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
                    val v = requested.version.orEmpty()
                    if (compareVersions(v, "1.21") >= 0 && compareVersions(v, "1.26.0") < 0) {
                        useVersion("1.26.0")
                    }
                }
                // AGP sdklib is using a vulnerable commons-lang3 version (CVE-2025-48924)
                if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
                    useVersion("3.18.0")
                }
                // AGP build tools pull in Bouncy Castle with a covert timing channel vulnerability
                // affecting FrodoEngine (CVE-2026-5598 / GHSA-p93r-85wp-75v3); fixed in 1.84.
                if (requested.group == "org.bouncycastle" && isOlderThan(requested.version, "1.84")) {
                    useVersion("1.84")
                }
                // GHSA-72hv-8253-57qq — Dokka and test configurations pull in older jackson
                if (requested.group.startsWith("com.fasterxml.jackson") && isOlderThan(requested.version, "2.18.9")) {
                    useVersion("2.18.9")
                }
                // AIKIDO-2025-10401 — Dokka pulls in older jsoup
                if (requested.group == "org.jsoup" && isOlderThan(requested.version, "1.21.1")) {
                    useVersion("1.21.1")
                }
            }
        }
    }
}

// Apply resolutionStrategy to all root project configurations
configurations.configureEach {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.netty" && requested.name == "netty-handler" && isOlderThan(requested.version, "4.1.135.Final")) {
                useVersion("4.1.135.Final")
            }
            // AIKIDO-2025-10401
            if (requested.group == "org.jsoup" && isOlderThan(requested.version, "1.21.1")) {
                useVersion("1.21.1")
            }
            // GHSA-5jmj-h7xm-6q6v
            if (requested.group.startsWith("com.fasterxml.jackson") && isOlderThan(requested.version, "2.18.9")) {
                useVersion("2.18.9")
            }
        }
    }
}

group = property("GROUP") as String
version = "4.0.0" // Fallback version

allprojects {
    group = rootProject.group
    // Setup project version to override from gradle.properties or fallback version
    version = rootProject.findProperty("VERSION_NAME") ?: rootProject.version
}

if (!rootProject.hasProperty("VERSION_NAME")) { // Disable this plugin if override is present
    apply(plugin = "com.dipien.semantic-version") // Plugin can be applied after version is defined
}

dependencies {
    dokka(project(":utilities"))
    dokka(project(":logger"))
    dokka(project(":logger-android"))
    dokka(project(":chat-sdk-core"))
    dokka(project(":chat-sdk-ui"))
}

dokka {
    moduleName.set("CXone Mobile SDK")
    dokkaPublications.named("html") {
        includes.from("README.md")
        outputDirectory.set(project.file("dist"))
    }
}

// This task mirrors the tests that will eventually be performed in various github actions
// and can be used as a final check before submitting a PR.
tasks.register("precheck") {
    // Syntax Checks
    dependsOn(":chat-sdk-core:check")
    dependsOn(":chat-sdk-ui:check")
    dependsOn(":store:check")
    dependsOn(":store:lintVitalRelease")
    // Build Check
    dependsOn(":store:assembleRelease")
}

rootCoverage {
    generateXml = true
    generateHtml = false
    excludes = listOf(
        "**/internal/model/**",
        "com/nice/cxonechat/sample/**",
        "com/nice/cxonechat/ui/**"
    )
}

tasks.register<Delete>("cleanProject") {
    delete(rootProject.layout.buildDirectory)
}

val reportMerge by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
    afterEvaluate {
        tasks.withType<Detekt>().configureEach {
            // Enable SARIF so the report file is actually written.
            reports.sarif.required.set(true)
            finalizedBy(reportMerge)
            // Use sarifReportFile (an @OutputFile declared directly on Detekt, not on the
            // nested DetektReport object) so Gradle 9 can resolve the owning task when wiring
            // the cross-project dependency into the root reportMerge input.
            reportMerge.configure { input.from(this@configureEach.sarifReportFile) }
        }
    }
}
