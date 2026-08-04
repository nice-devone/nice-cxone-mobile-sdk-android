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
    alias(libs.plugins.cxone.chat.java.library)
    alias(libs.plugins.cxone.chat.jvm.kotlin)
    alias(libs.plugins.cxone.chat.library.style)
    alias(libs.plugins.cxone.chat.test)
    alias(libs.plugins.cxone.chat.docs)
    alias(libs.plugins.cxone.chat.publish)
    alias(libs.plugins.dokka.javadoc)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xjvm-default=all-compatibility",
                "-Xjspecify-annotations=strict",
            )
        )
    }
}

java {
    withSourcesJar()
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.findProperty("POM_ARTIFACT_ID")?.toString() ?: project.name
                version = project.version.toString()

                artifact(tasks.named("javadocJar"))

                pom { project.configurePomMetadata(this) }
            }
        }
    }
}
