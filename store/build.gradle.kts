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

import com.nice.cxonechat.branchVersionForApp

plugins {
    alias(libs.plugins.cxone.chat.android.application)
    alias(libs.plugins.cxone.chat.android.ui)
    alias(libs.plugins.cxone.chat.koin)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.cxone.chat.android.test)
    alias(libs.plugins.cxone.chat.application.style)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization) apply true
    alias(libs.plugins.build.config)
}

val storeVersion: String = branchVersionForApp(version.toString())

android {
    namespace = "com.nice.cxonechat.sample"

    signingConfigs {
        named("debug") {
            storeFile = file("../keystore.jks")
            storePassword = "qwerty135"
            keyAlias = "key0"
            keyPassword = "qwerty135"
        }
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = "qwerty135"
            keyAlias = "key0"
            keyPassword = "qwerty135"
        }
    }

    defaultConfig {
        applicationId = "com.nice.cxonechat.sample"
        versionCode = 1
        versionName = storeVersion

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        named("debug") {
            versionNameSuffix = "-debug"
        }
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("shared/assets"))
        }
    }
    packaging {
        dex {
            useLegacyPackaging = false
        }
    }

    firebaseAppDistributionDefault {
        artifactType = "APK"
        releaseNotesFile = "docs/release/latest-release.md"
    }
}

androidComponents {
    onVariants { variant ->
        buildConfig.sourceSets.named(variant.name) {
            className.set("BuildConfig")
            packageName("com.nice.cxonechat.sample")
            buildConfigField(String::class.java, "VERSION_NAME", provider { storeVersion })
            buildConfigField(Boolean::class.java, "DEBUG", provider { variant.buildType == "debug" })
        }
    }
}

/* remove the superfluous files that happen to live in the shared assets directory */
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    doLast {
        outputs.files.asFileTree
            .filter { it.isFile && (it.extension == "md" || it.name.endsWith(".scheme.json")) }
            .forEach { it.delete() }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Koin
    implementation(libs.koin.compose.navigation3)

    // Initializer<T>
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.startup)

    // GoDaddy color picker
    implementation(libs.colorpicker)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Extended Emoji Support
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji.bundled)

    // AsyncImage
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.okhttp.logging)

    // CXone Chat SDK
    implementation(project(":chat-sdk-core"))
    implementation(project(":chat-sdk-ui"))
    implementation(project(":utilities"))
    implementation(project(":logger-android"))

    // AppAuth for OAuth 2.0 / OpenID Connect
    implementation(libs.openid.appauth)

    // Crashlytics
    implementation(libs.firebase.crashlytics)

    // Immutable annotations
    implementation(libs.findbugs)

    // Memory leak detection
    debugImplementation(libs.leakcanary.android)
}
