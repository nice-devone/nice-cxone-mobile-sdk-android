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
    alias(libs.plugins.kotlin.serialization) apply true
    alias(libs.plugins.dokka.javadoc)
}

android {
    namespace = "com.nice.cxonechat.log.client"

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":logger"))
    implementation(libs.androidx.ktx)
    implementation(libs.okhttp)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(tasks.named("javadocJar"))

                pom { project.configurePomMetadata(this) }
            }
        }
    }
}
