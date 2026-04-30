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

plugins {
    id("java-library-conventions")
    id("jvm-kotlin-conventions")
    id("library-style-conventions")
    id("test-conventions")
    id("docs-conventions")
    id("publish-conventions")
    id("org.jetbrains.dokka-javadoc")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xjvm-default=all-compatibility",
                "-Xjspecify-annotations=strict",
                "-Xtype-enhancement-improvements-strict-mode"
            )
        )
    }
}

java {
    withSourcesJar()
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationJavadoc"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.findProperty("POM_ARTIFACT_ID")?.toString() ?: project.name
                version = project.version.toString()

                artifact(javadocJar)

                pom {
                    project.extensions.extraProperties["configurePomMetadata"].let { it as groovy.lang.Closure<*> }.call(this)
                }
            }
        }
    }
}
