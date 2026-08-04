#!/bin/bash
#
# Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
#
# Licensed under the NICE License;
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
#
# TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
# AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
# OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
#

set -eo pipefail
set -x

PROFILE="-Pandroid.experimental.settings.executionProfile=high"

# Root project: gradle.lockfile, buildscript-gradle.lockfile, settings-gradle.lockfile
# dokkaGenerate is included defensively: Dokka's internal resolver configurations
# (dokkaHtmlGeneratorRuntimeResolver~internal, etc.) have previously required a
# dedicated run to lock correctly, even though the current Dokka version already
# captures them via `dependencies` alone.
./gradlew dependencies --write-locks $PROFILE

# build-logic is an included build (includeBuild); --write-locks does not propagate
# across included build boundaries, so it requires its own invocation.
# :convention:lint is required: `dependencies` alone leaves mainLintChecksClasspath,
# jvmTestCompileClasspathForLint, and jvmTestRuntimeClasspathForLint locked as empty.
# Running :convention:lint fresh (without --write-locks) against that lock state fails
# with "not part of the dependency lock state" errors.
./gradlew -p build-logic :convention:dependencies :convention:lint --write-locks $PROFILE

# All subprojects
./gradlew \
  :chat-sdk-core:dependencies \
  :chat-sdk-ui:dependencies \
  :store:dependencies \
  :utilities:dependencies \
  :logger-android:dependencies \
  :logger-client:dependencies \
  :logger:dependencies \
  :cxone-detekt-rules:dependencies \
  :chat-sdk-core-java:dependencies \
  --write-locks $PROFILE

