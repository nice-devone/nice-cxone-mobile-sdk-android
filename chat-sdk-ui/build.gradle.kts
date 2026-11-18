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

import com.nice.cxonechat.configurePomMetadata

plugins {
    alias(libs.plugins.cxone.chat.android.library)
    alias(libs.plugins.cxone.chat.android.ui)
    alias(libs.plugins.cxone.chat.android.kotlin)
    alias(libs.plugins.cxone.chat.koin)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.cxone.chat.android.docs)
    alias(libs.plugins.cxone.chat.android.test)
    alias(libs.plugins.cxone.chat.android.library.style)
    alias(libs.plugins.androidx.safeargs)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.cxone.chat.publish)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.dokka.javadoc)
}

android {
    namespace = "com.nice.cxonechat.ui"

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("debug") {
        }
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    koinCompiler {
        compileSafety = false // Right now not compatible with android test
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
                // Screenshot goldens hold formatted dates and times, and the formatters are process-scoped
                // ThreadLocals that freeze the zone and locale at construction, so a @Before pin cannot
                // reach them. Set the JVM defaults instead, before anything is built.
                it.systemProperties["user.timezone"] = "UTC"
                it.systemProperties["user.language"] = "en"
                it.systemProperties["user.country"] = "US"
                // Forks are separate JVMs, so org.gradle.jvmargs never reaches them and they would take
                // Gradle's 512m default. Only this module renders Compose through a Robolectric runtime, and
                // the aggregate test task runs modules in parallel, so this stays off the other eight.
                it.maxHeapSize = "2g"
                // running tests on Java 17 or higher, the JVM requires --add-opens flags
                // so that Robolectric can access internal OpenJDK classes and APIs
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
            }
        }
        managedDevices {
            localDevices {
                create("pixel2api35") {
                    device = "Pixel 2"
                    apiLevel = 35
                    systemImageSource = "aosp" // ADT flavored images don't work with accessibility tests
                }
            }
        }
    }

    publishing {
        multipleVariants("Maven") {
            allVariants()
            withSourcesJar()
        }
    }
}

dependencies {
    // Chat Activity is public API
    api(libs.androidx.compose.activity)
    // api, so the published api variant carries a version for the two Koin artifacts below
    api(platform(libs.koin.bom))
    api(libs.koin.android)
    api(libs.koin.annotations)
    // Handling of push notification sent via FCM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Lifecycle-process is used to suppress push notifications when app is in foreground
    implementation(libs.androidx.lifecycle)

    // Async Image & Video Frame
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.coil.video)

    // Zoomable composable elements like Image & AsyncImage
    implementation(libs.zoomable)

    // Multimedia message playback
    implementation(libs.androidx.compose.animation)
    implementation(libs.media.exoplayer)
    implementation(libs.media.exoplayer.hls)
    implementation(libs.media.datasource.okhttp)
    implementation(libs.media.ui.compose)

    // CXone Chat SDK
    api(project(":chat-sdk-core"))
    api(project(":logger"))
    implementation(project(":logger-android"))
    implementation(project(":utilities"))

    // Immutable annotations
    implementation(libs.findbugs)

    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)

    // Jetpack Compose - 1.8.0-alpha
    implementation(libs.androidx.compose.foundation)

    //  Lottie for animations
    implementation(libs.lottie.compose)

    // Emoji support
    implementation(libs.androidx.emoji2)

    // Support flexible layouts
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.window)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.emoji.bundled)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.coil.test)
    testImplementation(libs.media.test.utils)

    androidTestImplementation(libs.androidx.emoji.bundled)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.koin.android.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.coil.test)
    androidTestImplementation(libs.mockk.android)
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
        perPackageOption {
            matchingRegex.set(".*\\.internal.*|.*\\.generated.*")
            suppress.set(true)
        }
    }
}

roborazzi {
    outputDir.set(file("src/test/assets/screenshots"))
}

// Setup publishing configuration
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["Maven"])
                artifact(tasks.named("javadocJar"))

                pom { project.configurePomMetadata(this) }
            }
        }
    }
}
