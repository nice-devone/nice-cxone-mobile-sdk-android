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

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com.android.*")
                includeGroupByRegex("androidx.*")
                includeGroup("android.arch.lifecycle")
                includeGroup("android.arch.core")
                includeGroupByRegex("com.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    // Keep this AGP version in sync with `android-gradle-plugin` in `gradle/libs.versions.toml`.
    id("com.android.settings") version "9.2.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Aikido AutoFix injects a dependency mirror at the project level. Relax to
    // PREFER_SETTINGS only when both conditions hold: the aikido.autoFix Gradle property
    // is set AND Gradle is in --write-locks mode (the exact invocation Aikido uses).
    // This ensures the property never weakens normal builds even if it lingers.
    val aikidoAutofix =
        providers.gradleProperty("aikido.autoFix").orNull?.toBooleanStrictOrNull() == true &&
                gradle.startParameter.isWriteDependencyLocks
    repositoriesMode.set(if (aikidoAutofix) RepositoriesMode.PREFER_SETTINGS else RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    if (aikidoAutofix) logger.warn("Repository restrictions loosened for Aikido") else logger.info("Repository restrictions are strict")
    repositories {
        google {
            content {
                includeGroupByRegex("com.android.*")
                includeGroupByRegex("androidx.*")
                includeGroup("android.arch.lifecycle")
                includeGroup("android.arch.core")
                includeGroupByRegex("com.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "cxone-chat-sdk"

android {
    compileSdk = 37
    minSdk = 26
    execution {
        profiles {
            create("high") {
                r8 {
                    jvmOptions += listOf("-Xms2048m", "-Xmx8192m", "-XX:+HeapDumpOnOutOfMemoryError")
                    runInSeparateProcess = true
                }
            }
            create("low") {
                r8 {
                    jvmOptions += listOf("-Xms256m", "-Xmx2048m", "-XX:+HeapDumpOnOutOfMemoryError")
                    runInSeparateProcess = true
                }
            }
            create("ci") {
                r8 {
                    runInSeparateProcess = false
                }
            }
        }
        defaultProfile = "low"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":chat-sdk-core")
include(":chat-sdk-core-java")
include(":chat-sdk-ui")
include(":cxone-detekt-rules")
include(":store")
include(":utilities")
include(":logger")
include(":logger-android")
include(":logger-client")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    CXone Chat SDK requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}