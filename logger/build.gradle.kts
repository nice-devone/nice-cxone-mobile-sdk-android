/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("java-library-conventions")
    id("jvm-kotlin-conventions")
    id("library-style-conventions")
    id("test-conventions")
    id("docs-conventions")
    id("publish-conventions")
    id("api-conventions")
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            // whether to publish a sources jar
            sourcesJar = true,
        )
    )
}
