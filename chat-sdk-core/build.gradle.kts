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
    alias(libs.plugins.cxone.chat.android.kotlin)
    alias(libs.plugins.cxone.chat.android.docs)
    alias(libs.plugins.cxone.chat.android.test)
    alias(libs.plugins.cxone.chat.android.library.style)
    alias(libs.plugins.cxone.chat.publish)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.config)
    alias(libs.plugins.dokka.javadoc)
}

android {
    namespace = "com.nice.cxonechat.core"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("debug") {
            isMinifyEnabled = false
        }
        named("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("test").resources.srcDir("src/test/assets")
    }

    publishing {
        multipleVariants("Maven") {
            allVariants()
            withSourcesJar()
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api35") {
                    device = "Pixel 2"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        buildConfig.sourceSets.named(variant.name) {
            className.set("BuildConfig")
            packageName("com.nice.cxonechat.core")
            buildConfigField(String::class.java, "IDENTIFIER", provider { rootProject.name })
            buildConfigField(String::class.java, "VERSION_NAME", provider { project.version.toString() })
            buildConfigField(Boolean::class.java, "DEBUG", provider { variant.buildType == "debug" })
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xjspecify-annotations=strict",
        )
    }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.user.agent)
    implementation(project(":utilities"))
    api(project(":logger"))
    implementation(project(":logger-android"))
    implementation(project(":logger-client"))
    implementation(libs.androidx.datastore)
    implementation(libs.tink.android)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.reflection)
    testImplementation(libs.gson)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

dependencyLocking {
    lockAllConfigurations()
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
