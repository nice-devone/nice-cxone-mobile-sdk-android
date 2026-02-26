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

package com.nice.cxonechat.ui.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@MediumTest
class TemporaryFileProviderInstrumentedTest {

    @Test
    fun getAuthority_usesTestPackageName_provingDynamicConstruction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = TemporaryFileProvider.getAuthority(context)

        // Verify we're using the androidTest package, not the main module package
        assertEquals(
            "Test should use androidTest package (com.nice.cxonechat.ui.test), not main package (com.nice.cxonechat.ui)",
            "com.nice.cxonechat.ui.test",
            context.packageName
        )

        // Verify authority is constructed from the test package name
        assertEquals(
            "Authority should be dynamically constructed from test package name",
            "com.nice.cxonechat.ui.test.cxonechat.fileprovider",
            authority
        )

        // Verify it's NOT the hardcoded production value
        assertTrue(
            "Authority should NOT match hardcoded production value, proving dynamic construction",
            authority != "com.nice.cxonechat.fileprovider"
        )
    }

    @Test
    fun getUriForFile_withFilename_usesApplicationPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tmpDir = File(context.cacheDir, "tmp").apply { mkdirs() }
        val testFile = File(tmpDir, "test.txt")
        testFile.createNewFile()

        try {
            val uri = TemporaryFileProvider.getUriForFile(testFile, "test.txt", context)
            val expectedAuthority = TemporaryFileProvider.getAuthority(context)

            assertEquals(
                "Authority should match the dynamically constructed authority",
                expectedAuthority,
                uri.authority
            )
            assertTrue(
                "Authority should start with package name. Got: ${uri.authority}, Expected prefix: ${context.packageName}",
                uri.authority?.startsWith(context.packageName) == true
            )
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun getUriForFile_withoutFilename_usesApplicationPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tmpDir = File(context.cacheDir, "tmp").apply { mkdirs() }
        val testFile = File(tmpDir, "test.txt")
        testFile.createNewFile()

        try {
            val uri = TemporaryFileProvider.getUriForFile(testFile, context)
            val expectedAuthority = TemporaryFileProvider.getAuthority(context)

            assertEquals(
                "Authority should match the dynamically constructed authority",
                expectedAuthority,
                uri.authority
            )
            assertTrue(
                "Authority should start with package name. Got: ${uri.authority}, Expected prefix: ${context.packageName}",
                uri.authority?.startsWith(context.packageName) == true
            )
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun getUriForFile_producesCorrectAuthority() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tmpDir = File(context.cacheDir, "tmp").apply { mkdirs() }
        val testFile = File(tmpDir, "test.txt")
        testFile.createNewFile()

        try {
            val uri = TemporaryFileProvider.getUriForFile(testFile, context)
            val expectedAuthority = TemporaryFileProvider.getAuthority(context)

            assertEquals(
                "Authority should be constructed from package name with .cxonechat.fileprovider suffix",
                expectedAuthority,
                uri.authority
            )
        } finally {
            testFile.delete()
        }
    }
}
