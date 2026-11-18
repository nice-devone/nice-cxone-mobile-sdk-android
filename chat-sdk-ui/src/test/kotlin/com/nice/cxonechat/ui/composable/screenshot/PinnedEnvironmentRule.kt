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

package com.nice.cxonechat.ui.composable.screenshot

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.TimeZone

/**
 * Fails with an explanation when the timezone the goldens were recorded with is not in effect.
 *
 * `chat-sdk-ui/build.gradle.kts` pins the zone as a JVM default on the Gradle test task, which is the only
 * place that can reach the process-scoped `ThreadLocal` formatter caches. That leaves the IDE's own JUnit
 * runner unpinned, and on a machine outside UTC every golden holding a date or a clock fails as a pixel
 * diff, which says nothing about the cause. This turns that into the sentence below.
 *
 * Only the zone is checked. Robolectric derives the locale from `@Config(qualifiers)` rather than from
 * `user.language`, and the formatters read it back off the resource configuration, so it cannot drift with
 * the host and asserting on it would be a check that never fires.
 *
 * Read-only on purpose. Calling `TimeZone.setDefault` here would be too late for formatters already built
 * and would hide the misconfiguration rather than report it.
 */
internal class PinnedEnvironmentRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val zone = TimeZone.getDefault().id
            check(zone == EXPECTED_ZONE) {
                "Screenshot goldens were recorded in $EXPECTED_ZONE but this JVM is on $zone, so date and " +
                    "time labels will not match. Run these through Gradle, which pins the zone in " +
                    "chat-sdk-ui/build.gradle.kts."
            }
            base.evaluate()
        }
    }

    private companion object {
        const val EXPECTED_ZONE = "UTC"
    }
}
